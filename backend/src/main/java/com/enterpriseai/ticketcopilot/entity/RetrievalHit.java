package com.enterpriseai.ticketcopilot.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("retrieval_hit")
public class RetrievalHit {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String runId;
    private Integer rankOrder;
    private Long knowledgeArticleId;
    private String knowledgeArticleNo;
    private String knowledgeTitleSnapshot;
    private String knowledgeCategorySnapshot;
    private Integer score;
    private String matchedKeywordsSnapshot;
    private String excerptSnapshot;
    private Boolean usedInDraft;
    private LocalDateTime retrievedAt;
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

    public Integer getRankOrder() {
        return rankOrder;
    }

    public void setRankOrder(Integer rankOrder) {
        this.rankOrder = rankOrder;
    }

    public Long getKnowledgeArticleId() {
        return knowledgeArticleId;
    }

    public void setKnowledgeArticleId(Long knowledgeArticleId) {
        this.knowledgeArticleId = knowledgeArticleId;
    }

    public String getKnowledgeArticleNo() {
        return knowledgeArticleNo;
    }

    public void setKnowledgeArticleNo(String knowledgeArticleNo) {
        this.knowledgeArticleNo = knowledgeArticleNo;
    }

    public String getKnowledgeTitleSnapshot() {
        return knowledgeTitleSnapshot;
    }

    public void setKnowledgeTitleSnapshot(String knowledgeTitleSnapshot) {
        this.knowledgeTitleSnapshot = knowledgeTitleSnapshot;
    }

    public String getKnowledgeCategorySnapshot() {
        return knowledgeCategorySnapshot;
    }

    public void setKnowledgeCategorySnapshot(String knowledgeCategorySnapshot) {
        this.knowledgeCategorySnapshot = knowledgeCategorySnapshot;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getMatchedKeywordsSnapshot() {
        return matchedKeywordsSnapshot;
    }

    public void setMatchedKeywordsSnapshot(String matchedKeywordsSnapshot) {
        this.matchedKeywordsSnapshot = matchedKeywordsSnapshot;
    }

    public String getExcerptSnapshot() {
        return excerptSnapshot;
    }

    public void setExcerptSnapshot(String excerptSnapshot) {
        this.excerptSnapshot = excerptSnapshot;
    }

    public Boolean getUsedInDraft() {
        return usedInDraft;
    }

    public void setUsedInDraft(Boolean usedInDraft) {
        this.usedInDraft = usedInDraft;
    }

    public LocalDateTime getRetrievedAt() {
        return retrievedAt;
    }

    public void setRetrievedAt(LocalDateTime retrievedAt) {
        this.retrievedAt = retrievedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
