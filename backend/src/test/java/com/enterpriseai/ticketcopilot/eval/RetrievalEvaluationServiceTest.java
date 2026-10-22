package com.enterpriseai.ticketcopilot.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.enterpriseai.ticketcopilot.entity.KnowledgeArticle;
import com.enterpriseai.ticketcopilot.service.KnowledgeMatch;
import com.enterpriseai.ticketcopilot.service.KnowledgeMatchingService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;

class RetrievalEvaluationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void v2DatasetPinsTheCompleteDemoCorpusAndSeparatesNoMatchQueries() throws IOException {
        JsonNode dataset = new ObjectMapper().readTree(
            new ClassPathResource("eval/knowledge-retrieval-v2.json").getInputStream());
        Set<String> corpusArticleNos = new HashSet<>();
        dataset.path("corpusArticleNos").forEach(articleNo -> corpusArticleNos.add(articleNo.asText()));
        Set<String> caseIds = new HashSet<>();
        int positiveQueries = 0;
        int noMatchQueries = 0;
        int multiRelevantQueries = 0;

        for (JsonNode evalCase : dataset.path("cases")) {
            assertThat(caseIds.add(evalCase.path("id").asText())).isTrue();
            JsonNode relevantArticleNos = evalCase.path("relevantArticleNos");
            if (relevantArticleNos.isEmpty()) {
                noMatchQueries++;
                assertThat(evalCase.path("expectedArticleNo").isNull()).isTrue();
            } else {
                positiveQueries++;
                if (relevantArticleNos.size() > 1) {
                    multiRelevantQueries++;
                }
                for (JsonNode articleNo : relevantArticleNos) {
                    assertThat(corpusArticleNos).contains(articleNo.asText());
                }
                assertThat(relevantArticleNos).contains(evalCase.path("expectedArticleNo"));
            }
        }

        assertThat(dataset.path("version").asText()).isEqualTo("v2-full-corpus-qrels");
        assertThat(corpusArticleNos).containsExactlyInAnyOrder(
            "KB-ACCOUNT-001", "KB-API-500", "KB-DATA-004", "KB-DEPLOY-ENV",
            "KB-DRAFT-DEMO-0008", "KB-FAQ-KNOWLEDGE", "KB-IAM-002", "KB-IAM-ROLE",
            "KB-JAVA-PORT", "KB-MYSQL-SLOW", "KB-OPS-003", "KB-PROCESS-005",
            "KB-REDIS-CONN", "KB-SPRING-BEAN");
        assertThat(caseIds).hasSize(22);
        assertThat(positiveQueries).isEqualTo(18);
        assertThat(noMatchQueries).isEqualTo(4);
        assertThat(multiRelevantQueries).isGreaterThanOrEqualTo(2);
    }

    @Test
    void customDatasetWithoutAnExplicitOutputDoesNotReuseTheV2ReportPath() throws Exception {
        RetrievalEvaluationService v3Service = new RetrievalEvaluationService(
            mock(KnowledgeMatchingService.class),
            new ObjectMapper(),
            mock(ResourceLoader.class),
            3,
            "classpath:eval/knowledge-retrieval-v3-stress.json",
            ""
        );
        RetrievalEvaluationService v2Service = new RetrievalEvaluationService(
            mock(KnowledgeMatchingService.class),
            new ObjectMapper(),
            mock(ResourceLoader.class),
            3,
            "classpath:eval/knowledge-retrieval-v2.json",
            ""
        );

        Field outputPath = RetrievalEvaluationService.class.getDeclaredField("outputPath");
        outputPath.setAccessible(true);
        assertThat(outputPath.get(v3Service)).isEqualTo("target/evaluation/knowledge-retrieval-v3-stress-results.json");
        assertThat(outputPath.get(v2Service)).isEqualTo("target/evaluation/knowledge-retrieval-v2.json");
    }

    @Test
    void ranksMultipleRelevantArticlesAgainstTheExplicitFullCorpus() throws Exception {
        String dataset = """
            {
              "version":"v2-full-corpus-qrels",
              "source":"synthetic test fixture",
              "corpusVersion":"seed-v1",
              "corpusArticleNos":["KB-A","KB-B","KB-C","KB-X"],
              "cases":[
                {"id":"P1","query":"positive","queryType":"direct","expectedArticleNo":"KB-A","relevantArticleNos":["KB-A"]},
                {"id":"P2","query":"multi","queryType":"hard_negative","expectedArticleNo":"KB-B","relevantArticleNos":["KB-B","KB-C"]}
              ]
            }
            """;
        KnowledgeMatchingService matchingService = mock(KnowledgeMatchingService.class);
        when(matchingService.searchByQueryBaseline(eq("positive"), eq(3), any()))
            .thenReturn(List.of(match("KB-A"), match("KB-X"), match("KB-B")));
        when(matchingService.searchByQueryBaseline(eq("multi"), eq(3), any()))
            .thenReturn(List.of(match("KB-X"), match("KB-B"), match("KB-C")));

        RetrievalEvaluationService service = service(dataset, matchingService);
        RetrievalEvaluationService.EvaluationReport report = service.evaluateAndSave();
        ArgumentCaptor<Collection> corpora = ArgumentCaptor.forClass(Collection.class);
        verify(matchingService, times(2)).searchByQueryBaseline(anyString(), eq(3), corpora.capture());
        assertThat(corpora.getAllValues())
            .allSatisfy(corpus -> assertThat(corpus).containsExactlyInAnyOrder("KB-A", "KB-B", "KB-C", "KB-X"));

        JsonNode json = new ObjectMapper().valueToTree(report);
        assertThat(json.path("corpusArticleCount").asInt()).isEqualTo(4);
        assertThat(json.path("metrics").path("positiveQueryCount").asInt()).isEqualTo(2);
        assertThat(json.path("metrics").path("hitAt1").asInt()).isEqualTo(1);
        assertThat(json.path("metrics").path("hitAt3").asInt()).isEqualTo(2);
        assertThat(json.path("metrics").path("hitRateAt1").asDouble()).isEqualTo(0.5);
        assertThat(json.path("metrics").path("recallAt1").asDouble()).isEqualTo(0.5);
        assertThat(json.path("metrics").path("recallAt3").asDouble()).isEqualTo(1.0);
        assertThat(json.path("metrics").path("precisionAt3").asDouble()).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.000001));
        assertThat(json.path("metrics").path("meanReciprocalRank").asDouble()).isEqualTo(0.75);
    }

    @Test
    void countsCorrectNoMatchAndFalsePositiveRetrievalSeparately() throws Exception {
        String dataset = """
            {
              "version":"v2-full-corpus-qrels",
              "source":"synthetic test fixture",
              "corpusVersion":"seed-v1",
              "corpusArticleNos":["KB-A","KB-X"],
              "cases":[
                {"id":"N1","query":"no-results","queryType":"no_match","expectedArticleNo":null,"relevantArticleNos":[]},
                {"id":"N2","query":"false-positive","queryType":"no_match","expectedArticleNo":null,"relevantArticleNos":[]}
              ]
            }
            """;
        KnowledgeMatchingService matchingService = mock(KnowledgeMatchingService.class);
        when(matchingService.searchByQueryBaseline(eq("no-results"), eq(3), any())).thenReturn(List.of());
        when(matchingService.searchByQueryBaseline(eq("false-positive"), eq(3), any()))
            .thenReturn(List.of(match("KB-X")));

        JsonNode json = new ObjectMapper().valueToTree(service(dataset, matchingService).evaluateAndSave());

        assertThat(json.path("metrics").path("total").asInt()).isEqualTo(2);
        assertThat(json.path("metrics").path("positiveQueryCount").asInt()).isZero();
        assertThat(json.path("metrics").path("noMatchQueryCount").asInt()).isEqualTo(2);
        assertThat(json.path("metrics").path("noMatchTrueNegativeCount").asInt()).isEqualTo(1);
        assertThat(json.path("metrics").path("noMatchFalsePositiveCount").asInt()).isEqualTo(1);
        assertThat(json.path("metrics").path("noMatchSpecificity").asDouble()).isEqualTo(0.5);
        assertThat(json.path("cases").get(0).path("correctNoMatch").asBoolean()).isTrue();
        assertThat(json.path("cases").get(1).path("failureType").asText()).isEqualTo("UNEXPECTED_RETRIEVAL");
    }

    private RetrievalEvaluationService service(String dataset, KnowledgeMatchingService matchingService) throws Exception {
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        when(resourceLoader.getResource(anyString())).thenReturn(
            new ByteArrayResource(dataset.getBytes(StandardCharsets.UTF_8)));
        ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new RetrievalEvaluationService(
            matchingService,
            objectMapper,
            resourceLoader,
            3,
            "classpath:eval/knowledge-retrieval-v2.json",
            tempDir.resolve("retrieval-evaluation-test.json").toString()
        );
    }

    private static KnowledgeMatch match(String articleNo) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setArticleNo(articleNo);
        article.setTitle(articleNo);
        return new KnowledgeMatch(article, 90);
    }
}
