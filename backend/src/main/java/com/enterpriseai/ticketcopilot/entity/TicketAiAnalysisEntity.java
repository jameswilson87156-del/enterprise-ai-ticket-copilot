package com.enterpriseai.ticketcopilot.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("ticket_ai_analysis")
public class TicketAiAnalysisEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ticketId;
    private String classification;
    private String classificationReason;
    private Integer confidence;
    private String confirmationState;
    private String matchedKnowledgeNos;
    private String troubleshootingSteps;
    private String replySuggestion;
    private String riskNotes;
    private String sourceType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public String getClassificationReason() {
        return classificationReason;
    }

    public void setClassificationReason(String classificationReason) {
        this.classificationReason = classificationReason;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }

    public String getConfirmationState() {
        return confirmationState;
    }

    public void setConfirmationState(String confirmationState) {
        this.confirmationState = confirmationState;
    }

    public String getMatchedKnowledgeNos() {
        return matchedKnowledgeNos;
    }

    public void setMatchedKnowledgeNos(String matchedKnowledgeNos) {
        this.matchedKnowledgeNos = matchedKnowledgeNos;
    }

    public String getTroubleshootingSteps() {
        return troubleshootingSteps;
    }

    public void setTroubleshootingSteps(String troubleshootingSteps) {
        this.troubleshootingSteps = troubleshootingSteps;
    }

    public String getReplySuggestion() {
        return replySuggestion;
    }

    public void setReplySuggestion(String replySuggestion) {
        this.replySuggestion = replySuggestion;
    }

    public String getRiskNotes() {
        return riskNotes;
    }

    public void setRiskNotes(String riskNotes) {
        this.riskNotes = riskNotes;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
