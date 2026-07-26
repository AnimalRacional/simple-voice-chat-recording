package dev.omialien.voicechatrecording.taskscheduler;

import java.util.Comparator;
import java.util.PriorityQueue;

public class TaskScheduler {
    private long time;
    private final PriorityQueue<Task> tasks;
    public TaskScheduler() {
        this.time = 0;
        this.tasks = new PriorityQueue<>(Comparator.comparingLong(Task::time));
    }

    synchronized public void tick() {
        this.time++;
        while (!tasks.isEmpty() && tasks.peek().time() <= this.time) {
            tasks.poll().task().run();
        }
    }

    synchronized public void schedule(Runnable method, long after) {
        this.scheduleAt(method, time + after);
    }

    synchronized public void scheduleAt(Runnable method, long when) {
        Task task = new Task(when, method);
        tasks.add(task);
    }
}