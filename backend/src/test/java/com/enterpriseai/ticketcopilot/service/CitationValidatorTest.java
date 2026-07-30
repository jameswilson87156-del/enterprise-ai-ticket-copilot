package com.enterpriseai.ticketcopilot.service;

import java.time.LocalDateTime;
import java.util.List;

import com.enterpriseai.ticketcopilot.entity.RetrievalHit;
import com.enterpriseai.ticketcopilot.model.AbstentionReasonCode;
import com.enterpriseai.ticketcopilot.model.CitationRejectionReasonCode;
import com.enterpriseai.ticketcopilot.model.CitationValidationStatus;
import com.enterpriseai.ticketcopilot.model.RiskLevel;
import com.enterpriseai.ticketcopilot.model.StructuredCitation;
import com.enterpriseai.ticketcopilot.model.StructuredCopilotOutput;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CitationValidatorTest {

    private final CitationValidator validator = new CitationValidator();

    @Test
    void validatesCitationFromCurrentRunSnapshot() {
        CitationValidationResult result = validator.validate(output("KB-OPS-003"), List.of(hit("RUN-1", "KB-OPS-003", 1L)));

        assertThat(result.status()).isEqualTo(CitationValidationStatus.VALID);
        assertThat(result.validCitations()).hasSize(1);
        assertThat(result.validCitations().get(0).retrievalHit().getRunId()).isEqualTo("RUN-1");
    }

    @Test
    void rejectsCitationFromOtherRunWhenNotInCurrentSnapshot() {
        CitationValidationResult result = validator.validate(output("KB-OTHER-RUN"), List.of(hit("RUN-1", "KB-OPS-003", 1L)));

        assertThat(result.status()).isEqualTo(CitationValidationStatus.INVALID_CITATION);
        assertThat(result.rejectionReasonCode()).isEqualTo(CitationRejectionReasonCode.CITATION_NOT_IN_RUN);
    }

    @Test
    void rejectsKnowledgeArticleThatExistsButIsNotInTopKSnapshot() {
        CitationValidationResult result = validator.validate(output("KB-IAM-ROLE"), List.of(hit("RUN-1", "KB-OPS-003", 1L)));

        assertThat(result.status()).isEqualTo(CitationValidationStatus.INVALID_CITATION);
    }

    @Test
    void rejectsInternalNumericDatabaseIdEvenWhenItMatchesRetrievalHitPrimaryKey() {
        RetrievalHit hit = hit("RUN-1", "KB-OPS-003", 1L);
        hit.setKnowledgeArticleId(1L);

        CitationValidationResult result = validator.validate(output("1"), List.of(hit));

        assertThat(result.status()).isEqualTo(CitationValidationStatus.INVALID_CITATION);
        assertThat(result.rejectionReasonCode()).isEqualTo(CitationRejectionReasonCode.CITATION_NOT_IN_RUN);
    }

    @Test
    void requiresExactBusinessCitationIdWithoutCaseOrWhitespaceNormalization() {
        CitationValidationResult lowerCase = validator.validate(output("kb-ops-003"), List.of(hit("RUN-1", "KB-OPS-003", 1L)));
        CitationValidationResult decorated = validator.validate(output("KB-OPS-003 "), List.of(hit("RUN-1", "KB-OPS-003", 1L)));

        assertThat(lowerCase.status()).isEqualTo(CitationValidationStatus.INVALID_CITATION);
        assertThat(decorated.status()).isEqualTo(CitationValidationStatus.INVALID_CITATION);
    }

    @Test
    void rejectsInventedCitationId() {
        CitationValidationResult result = validator.validate(output("KB-NOT-REAL"), List.of(hit("RUN-1", "KB-OPS-003", 1L)));

        assertThat(result.status()).isEqualTo(CitationValidationStatus.INVALID_CITATION);
        assertThat(result.rejectedCitationCount()).isEqualTo(1);
    }

    @Test
    void deDuplicatesRepeatedValidCitation() {
        StructuredCopilotOutput output = new StructuredCopilotOutput(
            "answer",
            List.of(citation("KB-OPS-003"), citation("KB-OPS-003")),
            RiskLevel.LOW,
            false,
            List.of(),
            false,
            AbstentionReasonCode.NONE
        );

        CitationValidationResult result = validator.validate(output, List.of(hit("RUN-1", "KB-OPS-003", 1L)));

        assertThat(result.status()).isEqualTo(CitationValidationStatus.VALID);
        assertThat(result.validCitations()).hasSize(1);
        assertThat(result.rejectedCitationCount()).isEqualTo(1);
    }

    @Test
    void rejectsAbstentionWithCitation() {
        StructuredCopilotOutput output = new StructuredCopilotOutput(
            "abstain",
            List.of(citation("KB-OPS-003")),
            RiskLevel.MEDIUM,
            true,
            List.of(),
            true,
            AbstentionReasonCode.NO_RETRIEVAL_EVIDENCE
        );

        CitationValidationResult result = validator.validate(output, List.of(hit("RUN-1", "KB-OPS-003", 1L)));

        assertThat(result.status()).isEqualTo(CitationValidationStatus.INVALID_CITATION);
        assertThat(result.rejectionReasonCode()).isEqualTo(CitationRejectionReasonCode.CITATION_NOT_ALLOWED_FOR_ABSTENTION);
    }

    @Test
    void rejectsNonAbstentionWithoutCitation() {
        CitationValidationResult result = validator.validate(noCitationOutput(), List.of(hit("RUN-1", "KB-OPS-003", 1L)));

        assertThat(result.status()).isEqualTo(CitationValidationStatus.MISSING_CITATION);
        assertThat(result.rejectionReasonCode()).isEqualTo(CitationRejectionReasonCode.CITATION_REQUIRED);
    }

    @Test
    void rejectsAllCitationsWhenNoRetrievalHitExists() {
        CitationValidationResult result = validator.validate(output("KB-OPS-003"), List.of());

        assertThat(result.status()).isEqualTo(CitationValidationStatus.NO_RETRIEVAL_EVIDENCE);
        assertThat(result.rejectionReasonCode()).isEqualTo(CitationRejectionReasonCode.NO_RETRIEVAL_EVIDENCE);
    }

    @Test
    void usesRetrievalSnapshotAfterKnowledgeArticleMutation() {
        RetrievalHit snapshot = hit("RUN-1", "KB-OPS-003", 1L);
        snapshot.setKnowledgeTitleSnapshot("original title snapshot");

        CitationValidationResult result = validator.validate(output("KB-OPS-003"), List.of(snapshot));

        assertThat(result.status()).isEqualTo(CitationValidationStatus.VALID);
        assertThat(result.validCitations().get(0).retrievalHit().getKnowledgeTitleSnapshot()).isEqualTo("original title snapshot");
    }

    private StructuredCopilotOutput output(String citationId) {
        return new StructuredCopilotOutput(
            "answer",
            List.of(citation(citationId)),
            RiskLevel.MEDIUM,
            false,
            List.of(),
            false,
            AbstentionReasonCode.NONE
        );
    }

    private StructuredCopilotOutput noCitationOutput() {
        return new StructuredCopilotOutput(
            "answer",
            List.of(),
            RiskLevel.MEDIUM,
            false,
            List.of(),
            false,
            AbstentionReasonCode.NONE
        );
    }

    private StructuredCitation citation(String id) {
        return new StructuredCitation(id, "claim", "reason", "excerpt");
    }

    private RetrievalHit hit(String runId, String articleNo, Long id) {
        RetrievalHit hit = new RetrievalHit();
        hit.setId(id);
        hit.setRunId(runId);
        hit.setRankOrder(1);
        hit.setKnowledgeArticleId(100L);
        hit.setKnowledgeArticleNo(articleNo);
        hit.setKnowledgeTitleSnapshot("title snapshot");
        hit.setKnowledgeCategorySnapshot("????");
        hit.setScore(90);
        hit.setMatchedKeywordsSnapshot("[\"500\"]");
        hit.setExcerptSnapshot("snapshot excerpt");
        hit.setUsedInDraft(true);
        hit.setRetrievedAt(LocalDateTime.now());
        return hit;
    }
}
