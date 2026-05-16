package com.learning.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LearningPlan {
    public String id;
    public String name;
    public String description;
    public LocalDate startDate;
    public LocalDate expectedEndDate;
    public LocalDate actualEndDate;
    public Integer totalTasks;
    public Integer completedTasks;
    public Double completionRate;
    public Integer dailyTaskAmount;
    public Integer consecutiveCompletedDays;
    public Integer consecutiveIncompleteDays;
    public List<String> rewards = new ArrayList<>();
    public List<Task> tasks = new ArrayList<>();
    public List<TaskDependency> dependencies = new ArrayList<>();
    public List<Reminder> reminders = new ArrayList<>();
    public LocalDate createdAt;
    public LocalDate updatedAt;
}
