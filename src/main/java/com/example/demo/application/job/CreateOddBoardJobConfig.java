package com.example.demo.application.job;

import com.example.demo.application.job.param.CreateOddBoardJobParam;
import com.example.demo.domain.entity.Board;
import com.example.demo.domain.entity.OddBoard;
import com.example.demo.domain.repository.BoardRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Configuration
@Slf4j
public class CreateOddBoardJobConfig {
    private static final int CHUNK_SIZE = 1000;
    private static final int POOL_SIZE = 5;
    private static final int GRID_SIZE = 10;// 파티션 사이즈
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final CreateOddBoardJobParam createOddBoardJobParam;
    private final BoardRepository boardRepository;
    private final JdbcTemplate demoJdbcTemplate;

    public CreateOddBoardJobConfig(JobBuilderFactory jobBuilderFactory,
                                   StepBuilderFactory stepBuilderFactory,
                                   CreateOddBoardJobParam createOddBoardJobParam,
                                   BoardRepository boardRepository,
                                   @Qualifier("demoJdbcTemplate") JdbcTemplate demoJdbcTemplate) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.createOddBoardJobParam = createOddBoardJobParam;
        this.boardRepository = boardRepository;
        this.demoJdbcTemplate = demoJdbcTemplate;
    }

    @Bean
    public Job createOddBoardJob() {
        return jobBuilderFactory.get("createOddBoardJob")
                .start(createOddBoardStep())
                .build();
    }

    @Bean
    public Step createOddBoardStep() {
        return stepBuilderFactory.get("createOddBoardStep")
                .<Board, OddBoard>chunk(CHUNK_SIZE)
                .reader(createOddBoardReader())
                .processor(createOddBoardProcessor())
                .writer(createOddBoardWriter())
                .build();
    }

    @Bean
    @StepScope
    public RepositoryItemReader<Board> createOddBoardReader() {
        return new RepositoryItemReaderBuilder<Board>()
                .name("createBoardReader")
                .repository(boardRepository)
                .methodName("findAllByIdBetween")
                .arguments(createOddBoardJobParam.getMinId(),
                        createOddBoardJobParam.getMaxId())
                .pageSize(CHUNK_SIZE)
                .sorts(Map.of("id", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemProcessor<Board, OddBoard> createOddBoardProcessor() {
        LocalDateTime now = LocalDateTime.now();
        return board -> {
            if (board.getId() % 2 == 1) {
                return OddBoard.builder()
                        .id(board.getId())
                        .title(board.getTitle())
                        .content(board.getContent())
                        .createdAt(now)
                        .build();
            } else {
                return null;
            }
        };
    }

    @Bean
    public ItemWriter<OddBoard> createOddBoardWriter() {
        return oddBoards -> demoJdbcTemplate.batchUpdate("insert into OddBoard (id, title, content, createdAt) values (?, ?, ?, ?)",
                oddBoards,
                CHUNK_SIZE,
                (ps, oddBoard) -> {
                    ps.setObject(1, oddBoard.getId());
                    ps.setObject(2, oddBoard.getTitle());
                    ps.setObject(3, oddBoard.getContent());
                    ps.setObject(4, oddBoard.getCreatedAt());
                });
    }
}
