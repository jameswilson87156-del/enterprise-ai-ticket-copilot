package com.enterpriseai.ticketcopilot.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("copilot_run")
public class CopilotRun {

    @TableId(value = "run_id", type = IdType.INPUT)
    private String runId;
    private String traceId;
    private Long ticketId;
    private Long analysisId;
    private Long generationRecordId;
    private String requestedProvider;
    private String requestedProtocol;
    private String requestedModel;
    private String actualProvider;
    private String actualProtocol;
    private String runStatus;
    private Boolean fallbackUsed;
    private String fallbackReasonCode;
    private String errorCategory;
    private String sanitizedErrorSummary;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long totalLatencyMs;
    private Integer retrievalHitCount;
    private Boolean outputProduced;
    private Boolean humanReviewRequired;
    private LocalDateTime createdAt;

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Long analysisId) {
        this.analysisId = analysisId;
    }

    public Long getGenerationRecordId() {
        return generationRecordId;
    }

    public void setGenerationRecordId(Long generationRecordId) {
        this.generationRecordId = generationRecordId;
    }

    public String getRequestedProvider() {
        return requestedProvider;
    }

    public void setRequestedProvider(String requestedProvider) {
        this.requestedProvider = requestedProvider;
    }

    public String getRequestedProtocol() {
        return requestedProtocol;
    }

    public void setRequestedProtocol(String requestedProtocol) {
        this.requestedProtocol = requestedProtocol;
    }

    public String getRequestedModel() {
        return requestedModel;
    }

    public void setRequestedModel(String requestedModel) {
        this.requestedModel = requestedModel;
    }

    public String getActualProvider() {
        return actualProvider;
    }

    public void setActualProvider(String actualProvider) {
        this.actualProvider = actualProvider;
    }

    public String getActualProtocol() {
        return actualProtocol;
    }

    public void setActualProtocol(String actualProtocol) {
        this.actualProtocol = actualProtocol;
    }

    public String getRunStatus() {
        return runStatus;
    }

    public void setRunStatus(String runStatus) {
        this.runStatus = runStatus;
    }

    public Boolean getFallbackUsed() {
        return fallbackUsed;
    }

    public void setFallbackUsed(Boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }

    public String getFallbackReasonCode() {
        return fallbackReasonCode;
    }

    public void setFallbackReasonCode(String fallbackReasonCode) {
        this.fallbackReasonCode = fallbackReasonCode;
    }

    public String getErrorCategory() {
        return errorCategory;
    }

    public void setErrorCategory(String errorCategory) {
        this.errorCategory = errorCategory;
    }

    public String getSanitizedErrorSummary() {
        return sanitizedErrorSummary;
    }

    public void setSanitizedErrorSummary(String sanitizedErrorSummary) {
        this.sanitizedErrorSummary = sanitizedErrorSummary;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getTotalLatencyMs() {
        return totalLatencyMs;
    }

    public void setTotalLatencyMs(Long totalLatencyMs) {
        this.totalLatencyMs = totalLatencyMs;
    }

    public Integer getRetrievalHitCount() {
        return retrievalHitCount;
    }

    public void setRetrievalHitCount(Integer retrievalHitCount) {
        this.retrievalHitCount = retrievalHitCount;
    }

    public Boolean getOutputProduced() {
        return outputProduced;
    }

    public void setOutputProduced(Boolean outputProduced) {
        this.outputProduced = outputProduced;
    }

    public Boolean getHumanReviewRequired() {
        return humanReviewRequired;
    }

    public void setHumanReviewRequired(Boolean humanReviewRequired) {
        this.humanReviewRequired = humanReviewRequired;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
