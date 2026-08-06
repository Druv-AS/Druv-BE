package com.dhruv.dto;

public class CoStudyRoomStateDto {
    private String roomId;
    private int activeParticipantsCount;
    private long secondsRemaining;
    private boolean isTimerRunning;
    private String currentPhase; // FOCUS_50MIN, BREAK_10MIN

    public CoStudyRoomStateDto() {}

    public CoStudyRoomStateDto(String roomId, int activeParticipantsCount, long secondsRemaining, boolean isTimerRunning, String currentPhase) {
        this.roomId = roomId;
        this.activeParticipantsCount = activeParticipantsCount;
        this.secondsRemaining = secondsRemaining;
        this.isTimerRunning = isTimerRunning;
        this.currentPhase = currentPhase;
    }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public int getActiveParticipantsCount() { return activeParticipantsCount; }
    public void setActiveParticipantsCount(int activeParticipantsCount) { this.activeParticipantsCount = activeParticipantsCount; }

    public long getSecondsRemaining() { return secondsRemaining; }
    public void setSecondsRemaining(long secondsRemaining) { this.secondsRemaining = secondsRemaining; }

    public boolean isTimerRunning() { return isTimerRunning; }
    public void setTimerRunning(boolean timerRunning) { isTimerRunning = timerRunning; }

    public String getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(String currentPhase) { this.currentPhase = currentPhase; }
}
