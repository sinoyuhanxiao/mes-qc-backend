package com.fps.svmes.models.sql.taskSchedule;

public enum TaskType {
    CRON(1),
    FUTURE(2),
    CANCEL(3);

    private final int type;

    TaskType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}