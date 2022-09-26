package com.example.demo.domain.repository;

import com.example.demo.domain.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Article, Long> {
}
