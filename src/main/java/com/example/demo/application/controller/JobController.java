package com.example.demo.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("jobs")
@RequiredArgsConstructor
public class JobController {

    private final Job createArticleJob;
    private final JobLauncher jobLauncher;

    @GetMapping("/create-articles")
    public void runCreateArticleJob() throws Exception {
        jobLauncher.run(createArticleJob, new JobParametersBuilder()
                .addDate("date", new Date())
                .toJobParameters());
    }
}
