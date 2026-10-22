package com.enterpriseai.ticketcopilot.eval;

import static org.assertj.core.api.Assertions.assertThat;

import com.enterpriseai.ticketcopilot.mapper.KnowledgeArticleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Verifies a separately selected challenge dataset through the real H2/MyBatis keyword path. */
@SpringBootTest(properties = {
    "ticket-copilot.ai.embedding-enabled=false",
    "ticket-copilot.evaluation.retrieval.enabled=false",
    "ticket-copilot.evaluation.retrieval.dataset=classpath:eval/knowledge-retrieval-v3-stress.json",
    "ticket-copilot.evaluation.retrieval.output=${java.io.tmpdir}/ticket-copilot-retrieval-v3-${random.uuid}.json"
})
@ActiveProfiles("smoke")
@Transactional
class RetrievalEvaluationV3H2IntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KnowledgeArticleMapper knowledgeArticleMapper;

    @Autowired
    private RetrievalEvaluationService evaluationService;

    @BeforeEach
    void seedPinnedDemoCorpus() {
        RetrievalEvaluationCorpusFixture.seedV2(jdbcTemplate, knowledgeArticleMapper);
    }

    @Test
    void evaluatesTheV3ChallengeSetThroughTheActualMapperAndScorer() throws Exception {
        RetrievalEvaluationService.EvaluationReport report = evaluationService.evaluateAndSave();

        assertThat(report.datasetVersion()).isEqualTo("v3-diagnostic-challenge-20261022");
        assertThat(report.corpusArticleCount()).isEqualTo(14);
        assertThat(report.metrics().total()).isEqualTo(20);
        assertThat(report.metrics().positiveQueryCount()).isEqualTo(12);
        assertThat(report.metrics().noMatchQueryCount()).isEqualTo(8);
        assertThat(report.cases()).hasSize(20);
        assertThat(report.cases()).extracting(RetrievalEvaluationService.CaseResult::id)
            .containsExactly(
                "S01", "S02", "S03", "S04", "S05", "S06", "S07", "S08", "S09", "S10", "S11", "S12",
                "N01", "N02", "N03", "N04", "N05", "N06", "N07", "N08"
            );
        RetrievalEvaluationService.CaseResult deploymentCase = report.cases().stream()
            .filter(result -> result.id().equals("S07"))
            .findFirst()
            .orElseThrow();
        assertThat(deploymentCase.expectedRank())
            .as("deployment retrieval should match documented ConfigMap, Secret and startup-parameter terms")
            .isGreaterThan(0);
        assertThat(report.metrics().noMatchTrueNegativeCount() + report.metrics().noMatchFalsePositiveCount())
            .isEqualTo(report.metrics().noMatchQueryCount());
        assertThat(report.cases()).allSatisfy(result -> {
            assertThat(result.query()).isNotBlank();
            assertThat(result.relevantArticleNos()).isNotNull();
            assertThat(result.relevantRanks()).hasSameSizeAs(result.relevantArticleNos());
            assertThat(result.retrieved()).isNotNull();
        });
    }
}
