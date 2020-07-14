package com.feed.news.controller;


import com.feed.news.crawler.JsoupParser;
import com.feed.news.entity.db.Article;
import com.feed.news.entity.db.XUser;
import com.feed.news.repository.ArticleRepo;
import com.feed.news.service.NewsFeedService;
import com.feed.news.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Controller
public class UserController {

    @Autowired
    OAuth2AuthorizedClientService authclientService;

    private final NewsFeedService feedService;
    private final ArticleRepo articleRepo;
    private final UserService userService;

    public UserController(NewsFeedService feedService, ArticleRepo articleRepo, UserService userService) {
        this.feedService = feedService;
        this.articleRepo = articleRepo;
        this.userService = userService;
    }


    @ModelAttribute("registrationForm")
    public XUser registrationForm() {
        return new XUser();
    }

    @RequestMapping(value={"/login"}, method = RequestMethod.GET)
    public ModelAndView login(){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("login");
        return modelAndView;
    }


    @RequestMapping(value="/registration", method = RequestMethod.GET)
    public ModelAndView registration(){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("registration");
        return modelAndView;
    }

    @RequestMapping(value = "/registration", method = RequestMethod.POST)
    public ModelAndView createNewUser(@ModelAttribute("registrationForm")  @Valid XUser user, BindingResult bindingResult) {

        ModelAndView modelAndView = new ModelAndView();
        Optional<XUser> userExists = userService.findUserByEmail(user.getEmail());

        if (user.getFull_name().isEmpty() || user.getEmail().isEmpty()) {
            bindingResult.rejectValue("full_name", "error.user", "Each field is mandatory");
        }
        if (userExists.isPresent()) {
            bindingResult.rejectValue("email", "error.user", "There is already a user registered with the email provided");
        }
        if (!user.getPassword().equals(user.getConfirm_password())){
           bindingResult.rejectValue("password", "error.user", "The password fields must match");
        }
        if (bindingResult.hasErrors()) {
            modelAndView.setViewName("registration");
        }
        else {
            userService.saveUser(user);
            modelAndView.addObject("successMessage", "User has been registered successfully");
        }
        modelAndView.setViewName("registration");
        return  modelAndView;
    }

    @RequestMapping(value ={"/news"}, method = RequestMethod.GET)
    public ModelAndView showDesignForm(Model model) {

        ModelAndView modelAndView= new ModelAndView();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User user = (User) authentication.getPrincipal();

        int id=userService.findUserByEmail(user.getUsername()).get().getUser_id();

        log.info("user id: "+id);

        Stream<JsoupParser> newsParsers = feedService.getNewsParsers(id);
        List<Article> articles = newsParsers.flatMap(p -> p.getArticles().stream()).collect(Collectors.toList());
        articleRepo.saveAll(articles);
        model.addAttribute("articles", articles);
        modelAndView.setViewName("news");
        return modelAndView;
    }

    @RequestMapping(value="/oauth2LoginSuccess")
    public ModelAndView getOauth2LoginInfo(Model model,@AuthenticationPrincipal OAuth2AuthenticationToken authenticationToken) {
        ModelAndView modelAndView= new ModelAndView();

        // fetching the client details and user details and then adding them into
        System.out.println(authenticationToken.getAuthorizedClientRegistrationId()); // client name like facebook, google etc.
        System.out.println(authenticationToken.getName()); // facebook/google userId

        //	1.Fetching User Info
        OAuth2User user = authenticationToken.getPrincipal(); // When you work with Spring OAuth it gives you OAuth2User instead of UserDetails
        System.out.println("userId: "+user.getName()); // returns the userId of facebook
        // getAttributes map Contains User details like name, email etc// print the whole map for more details
        System.out.println("Email:"+ user.getAttributes().get("email"));

        //2. Just in case if you want to obtain User's auth token value, refresh token, expiry date etc you can use below snippet
        OAuth2AuthorizedClient client = authclientService.loadAuthorizedClient(authenticationToken.getAuthorizedClientRegistrationId(), authenticationToken.getName());
        System.out.println("Token Value"+ client.getAccessToken().getTokenValue());

        //3. Now you have full control on users data.You can either see if user is not present in Database then store it and
        // send welcome email for the first time
        model.addAttribute("name", user.getAttribute("name"));

        modelAndView.setViewName("news");
        return modelAndView;
    }

//    @GetMapping(value = {"/"})
//    public String home(Model model, @AuthenticationPrincipal Authentication authentication) {
//        System.out.println("Request appeared on server.");
//        // authentication's principle could be either through OAuth or via form-based so you have to cast the principle object into User object carefully.
//        if(authentication.getPrincipal() instanceof UserDetails) {
//            System.out.println("It was a form based login");
//            UserDetails user = (UserDetails) authentication.getPrincipal();
//            model.addAttribute("name", user.getUsername());
//
//        } else  {
//            System.out.println("It was OAuth based login");
//            OAuth2User user = (OAuth2User) authentication.getPrincipal();
//            model.addAttribute("name", user.getAttribute("name"));
//        }
//
//        return "news";
//    }
}
