package com.learning.service;

import com.learning.entity.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class DailyTaskScheduler {

    @Autowired
    private LearningPlanService learningPlanService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void generateDailyTasks() {
        LocalDate today = LocalDate.now();
        learningPlanService.getAllPlans().forEach(plan -> {
            plan.tasks.stream()
                    .filter(task -> task.scheduledDate.isBefore(today) && task.status != Task.TaskStatus.COMPLETED)
                    .forEach(task -> task.status = Task.TaskStatus.EXPIRED);
        });
    }
}
