package com.enterpriseai.ticketcopilot.eval;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.enterpriseai.ticketcopilot.service.KnowledgeMatch;
import com.enterpriseai.ticketcopilot.service.KnowledgeMatchingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

/** Runs a deterministic keyword-baseline evaluation over the checked-in synthetic dataset. */
@Service
public class RetrievalEvaluationService {

    private static final String DATASET_LOCATION = "classpath:eval/knowledge-retrieval-v1.json";

    private final KnowledgeMatchingService knowledgeMatchingService;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final int topK;
    private final String outputPath;

    public RetrievalEvaluationService(
        KnowledgeMatchingService knowledgeMatchingService,
        ObjectMapper objectMapper,
        ResourceLoader resourceLoader,
        @Value("${ticket-copilot.evaluation.retrieval.top-k:5}") int topK,
        @Value("${ticket-copilot.evaluation.retrieval.output:target/evaluation/knowledge-retrieval-v1.json}") String outputPath
    ) {
        this.knowledgeMatchingService = knowledgeMatchingService;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.topK = topK;
        this.outputPath = outputPath;
    }

    public EvaluationReport evaluateAndSave() throws Exception {
        if (topK < 3) {
            throw new IllegalArgumentException("ticket-copilot.evaluation.retrieval.top-k must be at least 3");
        }

        Resource datasetResource = resourceLoader.getResource(DATASET_LOCATION);
        EvalDataset dataset;
        try (var input = datasetResource.getInputStream()) {
            dataset = objectMapper.readValue(input, EvalDataset.class);
        }
        if (dataset.cases() == null || dataset.cases().isEmpty()) {
            throw new IllegalStateException("Retrieval evaluation dataset contains no cases");
        }

        Set<String> evaluationCorpus = dataset.cases().stream()
            .map(EvalCase::expectedArticleNo)
            .collect(Collectors.toUnmodifiableSet());
        List<CaseResult> caseResults = dataset.cases().stream()
            .map(evalCase -> evaluateCase(evalCase, evaluationCorpus))
            .toList();

        int total = caseResults.size();
        int hitAt1 = (int) caseResults.stream().filter(CaseResult::hitAt1).count();
        int hitAt3 = (int) caseResults.stream().filter(CaseResult::hitAt3).count();
        double reciprocalRankTotal = caseResults.stream()
            .mapToDouble(result -> result.expectedRank() == 0 ? 0.0 : 1.0 / result.expectedRank())
            .sum();
        EvaluationReport report = new EvaluationReport(
            Instant.now().toString(),
            dataset.version(),
            dataset.source(),
            "keyword-baseline",
            topK,
            new Metrics(
                total,
                hitAt1,
                hitAt3,
                ratio(hitAt1, total),
                ratio(hitAt3, total),
                reciprocalRankTotal / total
            ),
            caseResults
        );

        Path output = Path.of(outputPath);
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), report);
        return report;
    }

    private CaseResult evaluateCase(EvalCase evalCase, Set<String> evaluationCorpus) {
        List<KnowledgeMatch> matches = knowledgeMatchingService.searchByQueryBaseline(
            evalCase.query(), topK, evaluationCorpus);
        int rank = IntStream.range(0, matches.size())
            .filter(index -> evalCase.expectedArticleNo().equals(matches.get(index).article().getArticleNo()))
            .map(index -> index + 1)
            .findFirst()
            .orElse(0);

        boolean hitOne = rank == 1;
        boolean hitThree = rank > 0 && rank <= 3;
        List<RetrievedArticle> retrieved = matches.stream()
            .map(match -> new RetrievedArticle(
                match.article().getArticleNo(),
                match.article().getTitle(),
                match.relevance()
            ))
            .toList();
        String failureType = rank == 0
            ? "EXPECTED_ARTICLE_NOT_RETRIEVED"
            : rank == 1 ? "NONE" : "EXPECTED_ARTICLE_RANKED_BELOW_FIRST";
        return new CaseResult(
            evalCase.id(),
            evalCase.query(),
            evalCase.queryType(),
            evalCase.expectedArticleNo(),
            rank,
            hitOne,
            hitThree,
            failureType,
            retrieved
        );
    }

    private static double ratio(int numerator, int denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }

    public record EvalDataset(String version, String source, List<EvalCase> cases) {
    }

    public record EvalCase(String id, String query, String queryType, String expectedArticleNo) {
    }

    public record EvaluationReport(
        String generatedAt,
        String datasetVersion,
        String dataSource,
        String retrievalMode,
        int topK,
        Metrics metrics,
        List<CaseResult> cases
    ) {
    }

    public record Metrics(
        int total,
        int hitAt1,
        int hitAt3,
        double recallAt1,
        double recallAt3,
        double meanReciprocalRank
    ) {
    }

    public record CaseResult(
        String id,
        String query,
        String queryType,
        String expectedArticleNo,
        int expectedRank,
        boolean hitAt1,
        boolean hitAt3,
        String failureType,
        List<RetrievedArticle> retrieved
    ) {
    }

    public record RetrievedArticle(String articleNo, String title, int relevance) {
    }
}
