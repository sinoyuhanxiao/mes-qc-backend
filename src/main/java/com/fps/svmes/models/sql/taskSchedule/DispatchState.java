package com.fps.svmes.models.sql.taskSchedule;

import lombok.Getter;

@Getter
public enum DispatchState {
    Active((short) 1),
    Inactive((short) 2),
    Expired((short) 3),
    Exhausted((short) 4),
    Paused((short) 5);

    private final short state;

    DispatchState(short state) {
        this.state = state;
    }

}
