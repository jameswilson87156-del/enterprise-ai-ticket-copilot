package com.enterpriseai.ticketcopilot.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.enterpriseai.ticketcopilot.entity.RetrievalHit;
import com.enterpriseai.ticketcopilot.model.CitationRejectionReasonCode;
import com.enterpriseai.ticketcopilot.model.CitationValidationStatus;
import com.enterpriseai.ticketcopilot.model.StructuredCitation;
import com.enterpriseai.ticketcopilot.model.StructuredCopilotOutput;
import org.springframework.stereotype.Service;

@Service
public class CitationValidator {

    public CitationValidationResult validate(StructuredCopilotOutput output, List<RetrievalHit> retrievalHits) {
        List<RetrievalHit> hits = retrievalHits == null ? List.of() : retrievalHits;
        List<StructuredCitation> citations = output == null || output.citations() == null ? List.of() : output.citations();
        if (output != null && output.abstained()) {
            if (!citations.isEmpty()) {
                return CitationValidationResult.invalid(
                    CitationValidationStatus.INVALID_CITATION,
                    citations.size(),
                    CitationRejectionReasonCode.CITATION_NOT_ALLOWED_FOR_ABSTENTION
                );
            }
            return CitationValidationResult.notApplicable();
        }
        if (hits.isEmpty()) {
            return CitationValidationResult.invalid(
                CitationValidationStatus.NO_RETRIEVAL_EVIDENCE,
                citations.size(),
                CitationRejectionReasonCode.NO_RETRIEVAL_EVIDENCE
            );
        }
        if (citations.isEmpty()) {
            return CitationValidationResult.invalid(
                CitationValidationStatus.MISSING_CITATION,
                0,
                CitationRejectionReasonCode.CITATION_REQUIRED
            );
        }
        Map<String, RetrievalHit> allowedByKnowledgeId = new LinkedHashMap<>();
        for (RetrievalHit hit : hits) {
            if (hit.getKnowledgeArticleNo() != null && !hit.getKnowledgeArticleNo().isBlank()) {
                allowedByKnowledgeId.put(hit.getKnowledgeArticleNo(), hit);
            }
            if (hit.getKnowledgeArticleId() != null) {
                allowedByKnowledgeId.put(String.valueOf(hit.getKnowledgeArticleId()), hit);
            }
        }

        Map<String, CitationValidationResult.ValidatedCitation> validByKnowledgeId = new LinkedHashMap<>();
        int rejected = 0;
        for (StructuredCitation citation : citations) {
            String knowledgeId = citation.knowledgeArticleId() == null ? "" : citation.knowledgeArticleId().trim();
            RetrievalHit hit = allowedByKnowledgeId.get(knowledgeId);
            if (hit == null) {
                rejected++;
                continue;
            }
            String canonicalId = hit.getKnowledgeArticleNo() == null ? knowledgeId : hit.getKnowledgeArticleNo();
            validByKnowledgeId.putIfAbsent(canonicalId, new CitationValidationResult.ValidatedCitation(citation, hit));
        }
        int allowedMax = Math.min(StructuredOutputLimits.CITATION_MAX_COUNT, hits.size());
        if (validByKnowledgeId.size() > allowedMax) {
            return CitationValidationResult.invalid(
                CitationValidationStatus.LIMIT_EXCEEDED,
                validByKnowledgeId.size() - allowedMax,
                CitationRejectionReasonCode.LIMIT_EXCEEDED
            );
        }
        if (rejected > 0) {
            return CitationValidationResult.invalid(
                CitationValidationStatus.INVALID_CITATION,
                rejected,
                CitationRejectionReasonCode.CITATION_NOT_IN_RUN
            );
        }
        if (validByKnowledgeId.isEmpty()) {
            return CitationValidationResult.invalid(
                CitationValidationStatus.MISSING_CITATION,
                0,
                CitationRejectionReasonCode.CITATION_REQUIRED
            );
        }
        return CitationValidationResult.valid(new ArrayList<>(validByKnowledgeId.values()), citations.size() - validByKnowledgeId.size());
    }
}
