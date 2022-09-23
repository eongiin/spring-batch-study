package com.example.demo.application.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 읽어 올 파일 정보를 매핑할 객체
 */
@Getter
@Setter
public class ArticleModel {
    private String title;
    private String content;

}
