package com.enterpriseai.ticketcopilot.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("copilot_result_citation")
public class CopilotResultCitation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resultId;
    private String runId;
    private Long retrievalHitId;
    private String knowledgeArticleId;
    private String knowledgeTitleSnapshot;
    private String citationType;
    private String supportedClaim;
    private String evidenceExcerpt;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResultId() {
        return resultId;
    }

    public void setResultId(Long resultId) {
        this.resultId = resultId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public Long getRetrievalHitId() {
        return retrievalHitId;
    }

    public void setRetrievalHitId(Long retrievalHitId) {
        this.retrievalHitId = retrievalHitId;
    }

    public String getKnowledgeArticleId() {
        return knowledgeArticleId;
    }

    public void setKnowledgeArticleId(String knowledgeArticleId) {
        this.knowledgeArticleId = knowledgeArticleId;
    }

    public String getKnowledgeTitleSnapshot() {
        return knowledgeTitleSnapshot;
    }

    public void setKnowledgeTitleSnapshot(String knowledgeTitleSnapshot) {
        this.knowledgeTitleSnapshot = knowledgeTitleSnapshot;
    }

    public String getCitationType() {
        return citationType;
    }

    public void setCitationType(String citationType) {
        this.citationType = citationType;
    }

    public String getSupportedClaim() {
        return supportedClaim;
    }

    public void setSupportedClaim(String supportedClaim) {
        this.supportedClaim = supportedClaim;
    }

    public String getEvidenceExcerpt() {
        return evidenceExcerpt;
    }

    public void setEvidenceExcerpt(String evidenceExcerpt) {
        this.evidenceExcerpt = evidenceExcerpt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
