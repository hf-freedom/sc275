package com.learning.service;

import com.learning.dto.GoalAdjustRequest;
import com.learning.dto.PlanImportRequest;
import com.learning.entity.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class LearningPlanService {

    private final Map<String, LearningPlan> planStorage = new ConcurrentHashMap<>();
    private final Map<String, Task> taskStorage = new ConcurrentHashMap<>();

    public LearningPlan importPlan(PlanImportRequest request) {
        String planId = UUID.randomUUID().toString();
        LearningPlan plan = new LearningPlan();
        plan.id = planId;
        plan.name = request.name;
        plan.description = request.description;
        plan.startDate = request.startDate;
        plan.expectedEndDate = request.expectedEndDate;
        plan.dailyTaskAmount = request.dailyTaskAmount;
        plan.consecutiveCompletedDays = 0;
        plan.consecutiveIncompleteDays = 0;
        plan.completedTasks = 0;
        plan.createdAt = LocalDate.now();
        plan.updatedAt = LocalDate.now();

        List<Task> tasks = splitTasks(plan, request.tasks);
        plan.tasks = tasks;
        plan.totalTasks = tasks.size();
        plan.completionRate = 0.0;

        planStorage.put(planId, plan);
        tasks.forEach(task -> taskStorage.put(task.id, task));

        checkDependencies(plan);

        return plan;
    }

    private List<Task> splitTasks(LearningPlan plan, List<PlanImportRequest.TaskDefinition> taskDefinitions) {
        List<Task> tasks = new ArrayList<>();
        LocalDate currentDate = plan.startDate;
        int dailyCount = 0;
        int order = 0;

        Map<String, String> taskNameToId = new HashMap<>();

        for (PlanImportRequest.TaskDefinition def : taskDefinitions) {
            int daysNeeded = def.estimatedDays != null ? def.estimatedDays : 1;

            for (int day = 0; day < daysNeeded; day++) {
                Task task = new Task();
                task.id = UUID.randomUUID().toString();
                task.planId = plan.id;
                String taskName = daysNeeded > 1 ? def.name + " (第" + (day + 1) + "/" + daysNeeded + "部分)" : def.name;
                task.name = taskName;
                task.description = def.description;
                task.status = Task.TaskStatus.PENDING;
                task.level = Task.TaskLevel.valueOf(def.level.toUpperCase());
                task.order = order++;
                task.scheduledDate = currentDate;
                task.createdAt = LocalDate.now();
                task.updatedAt = LocalDate.now();
                tasks.add(task);
                taskNameToId.put(taskName, task.id);

                dailyCount++;
                if (dailyCount >= plan.dailyTaskAmount) {
                    currentDate = currentDate.plusDays(1);
                    dailyCount = 0;
                }
            }
        }

        for (Task task : tasks) {
            PlanImportRequest.TaskDefinition originalDef = taskDefinitions.stream()
                    .filter(d -> task.name.startsWith(d.name))
                    .findFirst()
                    .orElse(null);
            if (originalDef != null && originalDef.prerequisiteTaskNames != null) {
                List<String> prerequisiteIds = originalDef.prerequisiteTaskNames.stream()
                        .map(name -> taskNameToId.entrySet().stream()
                                .filter(e -> e.getKey().startsWith(name))
                                .map(Map.Entry::getValue)
                                .collect(Collectors.toList()))
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
                task.prerequisiteTaskIds = prerequisiteIds;
            }
        }

        return tasks;
    }

    public void updateTaskStatus(String taskId, boolean completed) {
        Task task = taskStorage.get(taskId);
        if (task == null) return;

        LearningPlan plan = planStorage.get(task.planId);
        if (plan == null) return;

        if (completed) {
            task.status = Task.TaskStatus.COMPLETED;
            task.completedDate = LocalDate.now();
            plan.completedTasks = plan.completedTasks + 1;
        } else {
            task.status = Task.TaskStatus.PENDING;
            if (task.completedDate != null) {
                plan.completedTasks = Math.max(0, plan.completedTasks - 1);
            }
        }
        task.updatedAt = LocalDate.now();

        updateCompletionRate(plan);
        updateConsecutiveDays(plan);
        adjustFutureTasks(plan);
        checkDependencies(plan);

        plan.updatedAt = LocalDate.now();
    }

    private void updateCompletionRate(LearningPlan plan) {
        if (plan.totalTasks > 0) {
            plan.completionRate = (double) plan.completedTasks / plan.totalTasks * 100;
        }
    }

    private void updateConsecutiveDays(LearningPlan plan) {
        LocalDate today = LocalDate.now();
        List<Task> todayTasks = plan.tasks.stream()
                .filter(t -> t.scheduledDate.equals(today))
                .collect(Collectors.toList());

        boolean allCompleted = todayTasks.stream()
                .allMatch(t -> t.status == Task.TaskStatus.COMPLETED);
        boolean noneCompleted = todayTasks.stream()
                .allMatch(t -> t.status != Task.TaskStatus.COMPLETED);

        if (allCompleted && !todayTasks.isEmpty()) {
            plan.consecutiveCompletedDays = plan.consecutiveCompletedDays + 1;
            plan.consecutiveIncompleteDays = 0;

            if (plan.consecutiveCompletedDays >= 3) {
                generateReward(plan);
                generateDifficultySuggestion(plan);
            }
        } else if (noneCompleted && !todayTasks.isEmpty() && today.isAfter(plan.startDate)) {
            plan.consecutiveIncompleteDays = plan.consecutiveIncompleteDays + 1;
            plan.consecutiveCompletedDays = 0;

            if (plan.consecutiveIncompleteDays >= 2) {
                int oldAmount = plan.dailyTaskAmount;
                reduceDailyTaskAmount(plan);
                addReminder(plan, Reminder.ReminderType.INCOMPLETE_WARNING,
                        "学习计划【" + plan.name + "】连续" + plan.consecutiveIncompleteDays + "天未完成任务，每日任务量已从 " + oldAmount + " 降低为 " + plan.dailyTaskAmount);
            }
        }
    }

    private void adjustFutureTasks(LearningPlan plan) {
        if (plan.consecutiveIncompleteDays > 0) {
            LocalDate today = LocalDate.now();
            List<Task> expiredTasks = plan.tasks.stream()
                    .filter(t -> t.scheduledDate.isBefore(today) && t.status != Task.TaskStatus.COMPLETED)
                    .collect(Collectors.toList());

            for (Task task : expiredTasks) {
                task.isMakeup = true;
                task.scheduledDate = today.plusDays(1);
                task.status = Task.TaskStatus.PENDING;
            }

            if (!expiredTasks.isEmpty()) {
                addReminder(plan, Reminder.ReminderType.MAKEUP_REQUIRED,
                        "有" + expiredTasks.size() + "个补学任务已安排到明天");
            }
        }
    }

    private void checkDependencies(LearningPlan plan) {
        for (Task task : plan.tasks) {
            if (task.status == Task.TaskStatus.PENDING || task.status == Task.TaskStatus.BLOCKED) {
                boolean prerequisitesMet = task.prerequisiteTaskIds.stream()
                        .allMatch(id -> {
                            Task prereq = taskStorage.get(id);
                            return prereq != null && prereq.status == Task.TaskStatus.COMPLETED;
                        });

                if (!prerequisitesMet && !task.prerequisiteTaskIds.isEmpty()) {
                    task.status = Task.TaskStatus.BLOCKED;
                    addReminder(plan, Reminder.ReminderType.TASK_BLOCKED,
                            "任务\"" + task.name + "\"因前置依赖未完成而被阻塞");
                } else if (task.status == Task.TaskStatus.BLOCKED) {
                    task.status = Task.TaskStatus.PENDING;
                }
            }
        }
    }

    private void reduceDailyTaskAmount(LearningPlan plan) {
        int newAmount = Math.max(1, plan.dailyTaskAmount - 1);
        plan.dailyTaskAmount = newAmount;
    }

    private void generateReward(LearningPlan plan) {
        List<String> rewards = Arrays.asList(
                "连续完成奖励：休息半天！",
                "太棒了！给自己买杯咖啡吧",
                "学习达人称号已解锁",
                "可以看一集喜欢的剧作为奖励",
                "效率之星！继续保持！"
        );
        String reward = rewards.get(new Random().nextInt(rewards.size()));
        plan.rewards.add(reward);
        addReminder(plan, Reminder.ReminderType.REWARD_EARNED, "获得奖励: " + reward);
    }

    private void generateDifficultySuggestion(LearningPlan plan) {
        addReminder(plan, Reminder.ReminderType.DIFFICULTY_INCREASE,
                "建议：当前进度良好，可考虑增加每日任务量或提高任务难度");
    }

    private void addReminder(LearningPlan plan, Reminder.ReminderType type, String message) {
        Reminder reminder = new Reminder();
        reminder.id = UUID.randomUUID().toString();
        reminder.planId = plan.id;
        reminder.type = type;
        reminder.message = message;
        reminder.createdAt = LocalDateTime.now();
        reminder.isRead = false;
        plan.reminders.add(reminder);
    }

    public LearningPlan adjustGoal(GoalAdjustRequest request) {
        LearningPlan plan = planStorage.get(request.planId);
        if (plan == null) return null;

        if (request.newExpectedEndDate != null) {
            plan.expectedEndDate = request.newExpectedEndDate;
        }
        if (request.newDailyTaskAmount != null) {
            plan.dailyTaskAmount = request.newDailyTaskAmount;
        }

        recalculateSchedule(plan);
        addReminder(plan, Reminder.ReminderType.GOAL_ADJUSTED, "学习目标已调整");
        plan.updatedAt = LocalDate.now();

        return plan;
    }

    private void recalculateSchedule(LearningPlan plan) {
        long totalDays = ChronoUnit.DAYS.between(plan.startDate, plan.expectedEndDate) + 1;
        int remainingTasks = (int) plan.tasks.stream()
                .filter(t -> t.status != Task.TaskStatus.COMPLETED)
                .count();

        if (totalDays > 0 && remainingTasks > 0) {
            int newDailyAmount = (int) Math.ceil((double) remainingTasks / totalDays);
            plan.dailyTaskAmount = Math.max(1, newDailyAmount);
        }
    }

    public Map<String, Object> getDailySummary() {
        Map<String, Object> summary = new HashMap<>();
        LocalDate today = LocalDate.now();

        List<Task> todayTasks = taskStorage.values().stream()
                .filter(t -> t.scheduledDate.equals(today))
                .collect(Collectors.toList());

        List<Task> expiredTasks = taskStorage.values().stream()
                .filter(t -> t.scheduledDate.isBefore(today) && t.status != Task.TaskStatus.COMPLETED)
                .collect(Collectors.toList());

        int totalTasks = taskStorage.size();
        int completedTasks = (int) taskStorage.values().stream()
                .filter(t -> t.status == Task.TaskStatus.COMPLETED)
                .count();

        summary.put("today", today);
        summary.put("todayTasks", todayTasks);
        summary.put("todayTaskCount", todayTasks.size());
        summary.put("todayCompletedCount", todayTasks.stream().filter(t -> t.status == Task.TaskStatus.COMPLETED).count());
        summary.put("expiredTasks", expiredTasks);
        summary.put("expiredTaskCount", expiredTasks.size());
        summary.put("totalTasks", totalTasks);
        summary.put("completedTasks", completedTasks);
        summary.put("completionRate", totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0);
        summary.put("plans", new ArrayList<>(planStorage.values()));

        return summary;
    }

    public List<LearningPlan> getAllPlans() {
        return new ArrayList<>(planStorage.values());
    }

    public LearningPlan getPlanById(String planId) {
        return planStorage.get(planId);
    }

    public List<Task> getTasksByPlanId(String planId) {
        return taskStorage.values().stream()
                .filter(t -> t.planId.equals(planId))
                .collect(Collectors.toList());
    }

    public List<Reminder> getUnreadReminders() {
        return planStorage.values().stream()
                .flatMap(p -> p.reminders.stream())
                .filter(r -> !r.isRead)
                .collect(Collectors.toList());
    }

    public void markReminderAsRead(String reminderId) {
        for (LearningPlan plan : planStorage.values()) {
            for (Reminder reminder : plan.reminders) {
                if (reminder.id.equals(reminderId)) {
                    reminder.isRead = true;
                    return;
                }
            }
        }
    }
}
