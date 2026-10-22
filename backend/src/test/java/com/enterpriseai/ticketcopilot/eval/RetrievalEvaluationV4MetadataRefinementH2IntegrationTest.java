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

/** Checks a narrow purchasing-FAQ keyword refinement against real H2/MyBatis retrieval. */
@SpringBootTest(properties = {
    "ticket-copilot.ai.embedding-enabled=false",
    "ticket-copilot.evaluation.retrieval.enabled=false",
    "ticket-copilot.evaluation.retrieval.dataset=classpath:eval/knowledge-retrieval-v4-metadata-regression.json",
    "ticket-copilot.evaluation.retrieval.output=${java.io.tmpdir}/ticket-copilot-retrieval-v4-${random.uuid}.json"
})
@ActiveProfiles("smoke")
@Transactional
class RetrievalEvaluationV4MetadataRefinementH2IntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KnowledgeArticleMapper knowledgeArticleMapper;

    @Autowired
    private RetrievalEvaluationService evaluationService;

    @BeforeEach
    void seedCurrentCandidateCorpus() {
        RetrievalEvaluationCorpusFixture.seedV3(jdbcTemplate, knowledgeArticleMapper);
    }

    @Test
    void removesThePrinterFalsePositiveWithoutLosingPurchasingFaqQueries() throws Exception {
        RetrievalEvaluationService.EvaluationReport report = evaluationService.evaluateAndSave();

        assertThat(report.datasetVersion()).isEqualTo("v4-keyword-metadata-regression-20261022");
        assertThat(report.metrics().total()).isEqualTo(22);
        assertThat(report.metrics().positiveQueryCount()).isEqualTo(14);
        assertThat(report.metrics().noMatchQueryCount()).isEqualTo(8);

        var printerNoMatch = report.cases().stream()
            .filter(result -> result.id().equals("V3-N02"))
            .findFirst()
            .orElseThrow();
        assertThat(printerNoMatch.correctNoMatch())
            .as("a printer repair query should not match the purchasing approval FAQ")
            .isTrue();
        assertThat(printerNoMatch.retrieved()).isEmpty();

        for (String caseId : new String[] {"V3-S08", "V2-R15", "V2-R16"}) {
            var purchasingQuery = report.cases().stream()
                .filter(result -> result.id().equals(caseId))
                .findFirst()
                .orElseThrow();
            assertThat(purchasingQuery.retrieved())
                .as("the purchasing FAQ should remain retrievable for %s", caseId)
                .extracting(RetrievalEvaluationService.RetrievedArticle::articleNo)
                .contains("KB-DRAFT-DEMO-0008");
        }
    }

}
