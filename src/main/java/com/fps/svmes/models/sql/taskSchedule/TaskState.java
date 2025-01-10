package com.fps.svmes.models.sql.taskSchedule;

import lombok.Getter;

import java.util.concurrent.locks.ReentrantLock;

public class TaskState {
    @Getter
    private final ReentrantLock lock = new ReentrantLock();
    private boolean isScheduled = false;

    public synchronized boolean isScheduled() {
        return isScheduled;
    }

    public synchronized void setScheduled(boolean scheduled) {
        isScheduled = scheduled;
    }
}
