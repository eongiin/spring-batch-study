package com.example.demo.application.scheduler;

import com.example.demo.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@RequiredArgsConstructor
public class JobScheduler {
    private final JobService jobService;

    @Scheduled(fixedDelay = 1000 * 5)// 끝난 후로부터 5초후
    public void runCreateArticleJob() throws Exception {
        jobService.runCreateArticleJob();
    }
}
