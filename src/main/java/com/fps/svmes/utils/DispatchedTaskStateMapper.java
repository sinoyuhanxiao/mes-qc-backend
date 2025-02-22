package com.fps.svmes.utils;

import java.util.HashMap;
import java.util.Map;

public class DispatchedTaskStateMapper {
    private static final Map<Short, String> stateMap = new HashMap<>();
    private static final Map<String, Short> reverseStateMap = new HashMap<>();

    static {
        stateMap.put((short) 1, "Pending");
        stateMap.put((short) 2, "In Progress");
        stateMap.put((short) 3, "Completed");
        stateMap.put((short) 4, "Canceled");
        stateMap.put((short) 5, "Overdue");

        // Reverse Mapping for easy lookup (state name -> stateId)
        for (Map.Entry<Short, String> entry : stateMap.entrySet()) {
            reverseStateMap.put(entry.getValue().toLowerCase(), entry.getKey());
        }
    }

    public static Map<Short, String> getAllStateMappings() {
        return new HashMap<>(stateMap); // Returns a copy of all state mappings
    }

    public static Short getStateId(String stateName) {
        return reverseStateMap.get(stateName.toLowerCase());
    }

    public static String getStateName(Short stateId) {
        return stateMap.get(stateId);
    }
}
