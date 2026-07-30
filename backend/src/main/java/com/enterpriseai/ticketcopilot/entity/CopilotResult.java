package com.enterpriseai.ticketcopilot.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("copilot_result")
public class CopilotResult {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String runId;
    private Long analysisId;
    private Long generationRecordId;
    private String answer;
    private Boolean abstained;
    private String abstentionReasonCode;
    private String riskLevel;
    private Boolean modelHumanReviewRequired;
    private Boolean finalHumanReviewRequired;
    private String citationValidationStatus;
    private String citationRejectionReasonCode;
    private Integer validCitationCount;
    private Integer rejectedCitationCount;
    private String outputValidationStatus;
    private String missingInformationJson;
    private String sourceType;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
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

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Boolean getAbstained() {
        return abstained;
    }

    public void setAbstained(Boolean abstained) {
        this.abstained = abstained;
    }

    public String getAbstentionReasonCode() {
        return abstentionReasonCode;
    }

    public void setAbstentionReasonCode(String abstentionReasonCode) {
        this.abstentionReasonCode = abstentionReasonCode;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Boolean getModelHumanReviewRequired() {
        return modelHumanReviewRequired;
    }

    public void setModelHumanReviewRequired(Boolean modelHumanReviewRequired) {
        this.modelHumanReviewRequired = modelHumanReviewRequired;
    }

    public Boolean getFinalHumanReviewRequired() {
        return finalHumanReviewRequired;
    }

    public void setFinalHumanReviewRequired(Boolean finalHumanReviewRequired) {
        this.finalHumanReviewRequired = finalHumanReviewRequired;
    }

    public String getCitationValidationStatus() {
        return citationValidationStatus;
    }

    public void setCitationValidationStatus(String citationValidationStatus) {
        this.citationValidationStatus = citationValidationStatus;
    }

    public String getCitationRejectionReasonCode() {
        return citationRejectionReasonCode;
    }

    public void setCitationRejectionReasonCode(String citationRejectionReasonCode) {
        this.citationRejectionReasonCode = citationRejectionReasonCode;
    }

    public Integer getValidCitationCount() {
        return validCitationCount;
    }

    public void setValidCitationCount(Integer validCitationCount) {
        this.validCitationCount = validCitationCount;
    }

    public Integer getRejectedCitationCount() {
        return rejectedCitationCount;
    }

    public void setRejectedCitationCount(Integer rejectedCitationCount) {
        this.rejectedCitationCount = rejectedCitationCount;
    }

    public String getOutputValidationStatus() {
        return outputValidationStatus;
    }

    public void setOutputValidationStatus(String outputValidationStatus) {
        this.outputValidationStatus = outputValidationStatus;
    }

    public String getMissingInformationJson() {
        return missingInformationJson;
    }

    public void setMissingInformationJson(String missingInformationJson) {
        this.missingInformationJson = missingInformationJson;
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
}
