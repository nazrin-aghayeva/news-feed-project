package com.feed.news.crawler.parsers;

import com.feed.news.crawler.RestTemplateService;
import com.feed.news.entity.db.Article;
import com.feed.news.crawler.DateTimeFormats;
import com.feed.news.crawler.JsoupParser;
import com.feed.news.crawler.Website;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TechStartupsParser extends RestTemplateService implements JsoupParser {

    List<Article> articles = new ArrayList<>();;
    Document doc;

//    public TechStartupsParser() {
//        this.articles = new ArrayList<>();
//        this.doc = rootPage("https://techstartups.com/");
//    }

    @SneakyThrows
    @Override
    public List<Article> getArticles() {
          Document doc = Jsoup.connect("https://techstartups.com/").get();
        Elements elements = doc.select(".sidebar_content .post");
        for (Element element : elements) {
            String header = element.select(".post_header_title").first().getElementsByTag("a").first().text();
            String content = element.select(".post_header_title > p").text();
            String link = element.select(".post_header_title").first()
                    .getElementsByTag("a").first().attr("href");
            String imageLink = element.getElementsByTag("img").first().attr("src");
            LocalDate date = convertStringToDate(element.select(".post_date .post_info_date > a").text().split("Posted On ")[1], DateTimeFormats.FULL_MONTH_FORMAT);

            articles.add(new Article(header, content, link, imageLink, date, Website.TechStartups));
        }

        return articles;
    }
}
