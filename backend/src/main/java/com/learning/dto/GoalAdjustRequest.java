package com.learning.dto;

import java.time.LocalDate;

public class GoalAdjustRequest {
    public String planId;
    public LocalDate newExpectedEndDate;
    public Integer newDailyTaskAmount;
}
