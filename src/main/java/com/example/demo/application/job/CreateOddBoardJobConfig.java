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
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDateTime;
import java.util.HashMap;
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
                .start(createOddBoardManager())
                .build();
    }

    @Bean
    public Step createOddBoardManager() {
        return stepBuilderFactory.get("createOddBoardManager")
                .partitioner("creaateOddBoardPartitioner", createOddBoardPartitioner())
                .partitionHandler(createOddBoardPartitionHandler())
                .build();
    }

    @Bean
    @StepScope
    public Partitioner createOddBoardPartitioner() {
        return gridSize -> {
            long min = createOddBoardJobParam.getMinId();
            long max = createOddBoardJobParam.getMaxId();
            long targetSize = (max - min) / gridSize + 1;

            Map<String, ExecutionContext> result = new HashMap<>();
            int number = 0;
            long start = min;
            long end = start + targetSize - 1;

            while (start <= max) {
                ExecutionContext value = new ExecutionContext();
                result.put("partition" + number, value);

                if (end >= max) {
                    end = max;
                }
                value.putLong("minValue", start);
                value.putLong("maxValue", end);
                start += targetSize;
                end += targetSize;
                number++;
            }

            return result;
        };
    }

    public TaskExecutorPartitionHandler createOddBoardPartitionHandler() {
        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setStep(createOddBoardWorker());
        partitionHandler.setGridSize(GRID_SIZE);
        partitionHandler.setTaskExecutor(createOddBoardTaskExecutor());
        return partitionHandler;
    }

    public ThreadPoolTaskExecutor createOddBoardTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(POOL_SIZE);
        taskExecutor.setMaxPoolSize(POOL_SIZE);
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.initialize();
        return taskExecutor;
    }

    @Bean
    public Step createOddBoardWorker() {
        return stepBuilderFactory.get("createOddBoardWorker")
                .<Board, OddBoard>chunk(CHUNK_SIZE)
                .reader(createOddBoardReader(null, null))
                .processor(createOddBoardProcessor())
                .writer(createOddBoardWriter())
                .build();
    }

    @Bean
    @StepScope
    public RepositoryItemReader<Board> createOddBoardReader(
            @Value("#{stepExecutionContext[minValue]}") Long minValue,
            @Value("#{stepExecutionContext[maxValue]}") Long maxValue
    ) {
        return new RepositoryItemReaderBuilder<Board>()
                .name("createBoardReader")
                .repository(boardRepository)
                .methodName("findAllByIdBetween")
                .arguments(minValue, maxValue)
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
