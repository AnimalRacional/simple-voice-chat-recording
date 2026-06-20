package dev.omialien.voicechatrecording.taskscheduler;

public record Task(long time, Runnable task) {}
