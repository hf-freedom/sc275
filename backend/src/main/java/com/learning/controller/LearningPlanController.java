package com.learning.controller;

import com.learning.dto.GoalAdjustRequest;
import com.learning.dto.PlanImportRequest;
import com.learning.dto.TaskUpdateRequest;
import com.learning.entity.LearningPlan;
import com.learning.entity.Reminder;
import com.learning.entity.Task;
import com.learning.service.LearningPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3004")
public class LearningPlanController {

    @Autowired
    private LearningPlanService learningPlanService;

    @PostMapping("/plans/import")
    public ResponseEntity<LearningPlan> importPlan(@RequestBody PlanImportRequest request) {
        LearningPlan plan = learningPlanService.importPlan(request);
        return ResponseEntity.ok(plan);
    }

    @GetMapping("/plans")
    public ResponseEntity<List<LearningPlan>> getAllPlans() {
        return ResponseEntity.ok(learningPlanService.getAllPlans());
    }

    @GetMapping("/plans/{planId}")
    public ResponseEntity<LearningPlan> getPlanById(@PathVariable String planId) {
        LearningPlan plan = learningPlanService.getPlanById(planId);
        if (plan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(plan);
    }

    @GetMapping("/plans/{planId}/tasks")
    public ResponseEntity<List<Task>> getTasksByPlanId(@PathVariable String planId) {
        return ResponseEntity.ok(learningPlanService.getTasksByPlanId(planId));
    }

    @PutMapping("/tasks/status")
    public ResponseEntity<Void> updateTaskStatus(@RequestBody TaskUpdateRequest request) {
        learningPlanService.updateTaskStatus(request.taskId, request.completed);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/plans/adjust")
    public ResponseEntity<LearningPlan> adjustGoal(@RequestBody GoalAdjustRequest request) {
        LearningPlan plan = learningPlanService.adjustGoal(request);
        if (plan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(plan);
    }

    @GetMapping("/daily-summary")
    public ResponseEntity<Map<String, Object>> getDailySummary() {
        return ResponseEntity.ok(learningPlanService.getDailySummary());
    }

    @GetMapping("/reminders/unread")
    public ResponseEntity<List<Reminder>> getUnreadReminders() {
        return ResponseEntity.ok(learningPlanService.getUnreadReminders());
    }

    @PutMapping("/reminders/{reminderId}/read")
    public ResponseEntity<Void> markReminderAsRead(@PathVariable String reminderId) {
        learningPlanService.markReminderAsRead(reminderId);
        return ResponseEntity.ok().build();
    }
}
