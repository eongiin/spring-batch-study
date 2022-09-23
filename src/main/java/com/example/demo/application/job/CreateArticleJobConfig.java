package com.example.demo.application.job;

import com.example.demo.application.model.ArticleModel;
import com.example.demo.domain.entity.Article;
import com.example.demo.domain.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class CreateArticleJobConfig {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final ArticleRepository articleRepository;
    private final JdbcTemplate jdbcTemplate;

    @Bean
    public Job createArticleJob() {
        return jobBuilderFactory.get("createArticleJob")
                .incrementer(new RunIdIncrementer())
                .start(createArticleStep())
                .build();
    }

    @Bean
    public Step createArticleStep() {
        return stepBuilderFactory.get("createArticleStep")
                .<ArticleModel, Article>chunk(1000)
                .reader(createArticleReader())
                .processor(createArticleProcessor())
                .writer(createArticleWriter())
                .build();
    }

    /**
     * Articles.csv 파일 읽기
     */
    @Bean
    public FlatFileItemReader<ArticleModel> createArticleReader() {
        return new FlatFileItemReaderBuilder<ArticleModel>()
                .name("createArticleReader")
                .resource(new ClassPathResource("Articles.csv"))
                .delimited()
                .names("title", "content")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>())
                .targetType(ArticleModel.class)
                .build();
    }

    /**
     * ArticleModel -> Article
     */
    @Bean
    public ItemProcessor<ArticleModel, Article> createArticleProcessor() {
        LocalDateTime now = LocalDateTime.now();
        return articleModel -> Article.builder()
                .title(articleModel.getTitle())
                .content(articleModel.getContent())
                .createdAt(now)
                .build();
    }

    /**
     * 데이터 db에 저장
     * entityManagerFactory나 Repository와 같은 JPA 관련된 클래스여야지만 사용 가능
     */
    @Bean
    public ItemWriter<Article> createArticleWriter() {
        return articles -> jdbcTemplate.batchUpdate("insert into Article (title, content, createdAt) values (?, ?, ?)",
                articles,
                1000,
                (ps, article) -> {
                    ps.setObject(1, article.getTitle());
                    ps.setObject(2, article.getContent());
                    ps.setObject(3, article.getCreatedAt());
                });
    }
}
