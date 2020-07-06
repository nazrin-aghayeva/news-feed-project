package com.feed.news.repository;

import com.feed.news.entity.db.XUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepo extends JpaRepository<XUser,Integer> {

}
