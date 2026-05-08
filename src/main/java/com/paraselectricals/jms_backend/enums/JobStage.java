package com.paraselectricals.jms_backend.enums;

public enum JobStage {
    RECEIVED(1, "Received"),
    INSPECTION(2, "Inspection"),
    DISMANTLING(3, "Dismantling"),
    REWINDING(4, "Rewinding"),
    COIL_MANUFACTURING(5, "Coil Manufacturing"),
    VPI(6, "VPI"),
    ASSEMBLY(7, "Assembly"),
    TESTING(8, "Testing"),
    READY_FOR_DISPATCH(9, "Ready for Dispatch"),
    DISPATCHED(10, "Dispatched");

    private final int stageNumber;
    private final String displayName;

    JobStage(int stageNumber, String displayName) {
        this.stageNumber = stageNumber;
        this.displayName = displayName;
    }

    public int getStageNumber() {
        return stageNumber;
    }

    public String getDisplayName() {
        return displayName;
    }
}
