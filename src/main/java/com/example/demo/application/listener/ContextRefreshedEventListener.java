package com.example.demo.application.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class ContextRefreshedEventListener implements ApplicationListener<org.springframework.context.event.ContextRefreshedEvent> {

    private final JobExplorer jobExplorer;
    private final JobRepository jobRepository;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Stop running jobs.");
        for (String jobName : jobExplorer.getJobNames()) {
            Set<JobExecution> runningJobExecutions = jobExplorer.findRunningJobExecutions(jobName);

            for (JobExecution jobExecution : runningJobExecutions) {
                jobExecution.setStatus(BatchStatus.STOPPED);
                jobExecution.setEndTime(new Date());
                for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
                    if (stepExecution.getStatus().isRunning()) {
                        stepExecution.setStatus(BatchStatus.STOPPED);
                        stepExecution.setEndTime(new Date());
                        jobRepository.update(stepExecution);
                    }
                }
                jobRepository.update(jobExecution);
                log.info("Update job execution status: {}", jobExecution.getJobId());
            }
        }
        log.info("Stopped running jobs.");
    }
}
