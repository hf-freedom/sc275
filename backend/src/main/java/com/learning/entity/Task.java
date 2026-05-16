package com.learning.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Task {
    public String id;
    public String planId;
    public String name;
    public String description;
    public TaskStatus status;
    public TaskLevel level;
    public LocalDate scheduledDate;
    public LocalDate completedDate;
    public Integer order;
    public Boolean isMakeup = false;
    public List<String> prerequisiteTaskIds = new ArrayList<>();
    public LocalDate createdAt;
    public LocalDate updatedAt;

    public enum TaskStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        EXPIRED,
        BLOCKED
    }

    public enum TaskLevel {
        BASIC,
        INTERMEDIATE,
        ADVANCED
    }
}
