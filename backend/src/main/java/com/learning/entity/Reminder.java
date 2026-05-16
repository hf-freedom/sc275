package com.learning.entity;

import java.time.LocalDateTime;

public class Reminder {
    public String id;
    public String planId;
    public String message;
    public ReminderType type;
    public LocalDateTime createdAt;
    public Boolean isRead = false;

    public enum ReminderType {
        INCOMPLETE_WARNING,
        TASK_BLOCKED,
        MAKEUP_REQUIRED,
        GOAL_ADJUSTED,
        REWARD_EARNED,
        DIFFICULTY_INCREASE
    }
}
