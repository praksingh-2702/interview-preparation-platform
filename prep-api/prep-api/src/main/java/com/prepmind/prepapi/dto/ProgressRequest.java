package com.prepmind.prepapi.dto;

public class ProgressRequest {
    private Long questionId;
    private boolean solved;
    private boolean revised;

    // Default constructor for Jackson deserialization
    public ProgressRequest() {}

    public ProgressRequest(Long questionId, boolean solved, boolean revised) {
        this.questionId = questionId;
        this.solved = solved;
        this.revised = revised;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public boolean isSolved() {
        return solved;
    }

    public void setSolved(boolean solved) {
        this.solved = solved;
    }

    public boolean isRevised() {
        return revised;
    }

    public void setRevised(boolean revised) {
        this.revised = revised;
    }
}