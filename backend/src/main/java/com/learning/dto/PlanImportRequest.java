package com.learning.dto;

import java.time.LocalDate;
import java.util.List;

public class PlanImportRequest {
    public String name;
    public String description;
    public LocalDate startDate;
    public LocalDate expectedEndDate;
    public Integer dailyTaskAmount;
    public List<TaskDefinition> tasks;

    public static class TaskDefinition {
        public String name;
        public String description;
        public String level;
        public List<String> prerequisiteTaskNames;
        public Integer estimatedDays;
    }
}
