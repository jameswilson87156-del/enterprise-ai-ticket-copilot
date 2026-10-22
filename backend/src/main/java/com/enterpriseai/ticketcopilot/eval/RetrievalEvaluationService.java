package com.enterpriseai.ticketcopilot.eval;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import com.enterpriseai.ticketcopilot.service.KnowledgeMatch;
import com.enterpriseai.ticketcopilot.service.KnowledgeMatchingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

/** Runs the deterministic keyword baseline over a versioned, pinned corpus and qrels. */
@Service
public class RetrievalEvaluationService {

    private static final String DEFAULT_DATASET_LOCATION = "classpath:eval/knowledge-retrieval-v2.json";

    private final KnowledgeMatchingService knowledgeMatchingService;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final int topK;
    private final String datasetLocation;
    private final String outputPath;

    public RetrievalEvaluationService(
        KnowledgeMatchingService knowledgeMatchingService,
        ObjectMapper objectMapper,
        ResourceLoader resourceLoader,
        @Value("${ticket-copilot.evaluation.retrieval.top-k:5}") int topK,
        @Value("${ticket-copilot.evaluation.retrieval.dataset:classpath:eval/knowledge-retrieval-v2.json}") String datasetLocation,
        @Value("${ticket-copilot.evaluation.retrieval.output:}") String outputPath
    ) {
        this.knowledgeMatchingService = knowledgeMatchingService;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.topK = topK;
        this.datasetLocation = datasetLocation == null || datasetLocation.isBlank()
            ? DEFAULT_DATASET_LOCATION
            : datasetLocation;
        this.outputPath = outputPath == null || outputPath.isBlank()
            ? defaultOutputLocation(this.datasetLocation)
            : outputPath;
    }

    public EvaluationReport evaluateAndSave() throws Exception {
        if (topK < 3) {
            throw new IllegalArgumentException("ticket-copilot.evaluation.retrieval.top-k must be at least 3");
        }

        EvalDataset dataset = readDataset();
        validateDataset(dataset);
        knowledgeMatchingService.requirePublishedArticles(dataset.corpusArticleNos());

        List<CaseResult> caseResults = dataset.cases().stream()
            .map(evalCase -> evaluateCase(evalCase, dataset.corpusArticleNos()))
            .toList();

        List<CaseResult> positiveResults = caseResults.stream()
            .filter(result -> !result.relevantArticleNos().isEmpty())
            .toList();
        List<CaseResult> noMatchResults = caseResults.stream()
            .filter(result -> result.relevantArticleNos().isEmpty())
            .toList();
        int hitAt1 = (int) positiveResults.stream().filter(CaseResult::hitAt1).count();
        int hitAt3 = (int) positiveResults.stream().filter(CaseResult::hitAt3).count();
        double recallAt1 = mean(positiveResults.stream().mapToDouble(CaseResult::recallAt1).toArray());
        double recallAt3 = mean(positiveResults.stream().mapToDouble(CaseResult::recallAt3).toArray());
        double precisionAt3 = mean(positiveResults.stream().mapToDouble(CaseResult::precisionAt3).toArray());
        double reciprocalRankTotal = positiveResults.stream()
            .mapToDouble(result -> result.firstRelevantRank() == 0 ? 0.0 : 1.0 / result.firstRelevantRank())
            .sum();
        int trueNegatives = (int) noMatchResults.stream().filter(CaseResult::correctNoMatch).count();
        int falsePositives = noMatchResults.size() - trueNegatives;

        EvaluationReport report = new EvaluationReport(
            Instant.now().toString(),
            dataset.version(),
            dataset.source(),
            dataset.corpusVersion(),
            dataset.corpusArticleNos().size(),
            "keyword-baseline",
            topK,
            new Metrics(
                caseResults.size(),
                positiveResults.size(),
                noMatchResults.size(),
                hitAt1,
                hitAt3,
                ratio(hitAt1, positiveResults.size()),
                ratio(hitAt3, positiveResults.size()),
                recallAt1,
                recallAt3,
                precisionAt3,
                positiveResults.isEmpty() ? 0.0 : reciprocalRankTotal / positiveResults.size(),
                trueNegatives,
                falsePositives,
                ratio(trueNegatives, noMatchResults.size())
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

    private EvalDataset readDataset() throws Exception {
        Resource datasetResource = resourceLoader.getResource(datasetLocation);
        try (var input = datasetResource.getInputStream()) {
            return objectMapper.readValue(input, EvalDataset.class);
        }
    }

    private static String defaultOutputLocation(String datasetLocation) {
        if (DEFAULT_DATASET_LOCATION.equals(datasetLocation)) {
            return "target/evaluation/knowledge-retrieval-v2.json";
        }
        String fileName = datasetLocation.substring(datasetLocation.lastIndexOf('/') + 1);
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex > 0) {
            fileName = fileName.substring(0, extensionIndex);
        }
        if (fileName.isBlank()) {
            fileName = "knowledge-retrieval-evaluation";
        }
        return "target/evaluation/" + fileName + "-results.json";
    }

    private static void validateDataset(EvalDataset dataset) {
        if (dataset == null || dataset.cases() == null || dataset.cases().isEmpty()) {
            throw new IllegalStateException("Retrieval evaluation dataset contains no cases");
        }
        if (dataset.corpusVersion() == null || dataset.corpusVersion().isBlank()
            || dataset.corpusArticleNos() == null || dataset.corpusArticleNos().isEmpty()) {
            throw new IllegalStateException("Retrieval evaluation dataset must pin a non-empty corpus and version");
        }

        Set<String> corpus = new HashSet<>();
        for (String articleNo : dataset.corpusArticleNos()) {
            if (articleNo == null || articleNo.isBlank() || !corpus.add(articleNo)) {
                throw new IllegalStateException("Retrieval evaluation corpus contains a blank or duplicate article number");
            }
        }
        Set<String> caseIds = new HashSet<>();
        for (EvalCase evalCase : dataset.cases()) {
            if (evalCase == null || evalCase.id() == null || evalCase.id().isBlank() || !caseIds.add(evalCase.id())) {
                throw new IllegalStateException("Retrieval evaluation dataset contains a blank or duplicate case ID");
            }
            if (evalCase.query() == null || evalCase.query().isBlank()
                || evalCase.relevantArticleNos() == null || evalCase.relevantArticleNos().stream().anyMatch(no -> no == null || no.isBlank())) {
                throw new IllegalStateException("Retrieval evaluation case must include a query and a non-null qrels list: " + evalCase.id());
            }
            Set<String> relevant = new HashSet<>(evalCase.relevantArticleNos());
            if (relevant.size() != evalCase.relevantArticleNos().size()) {
                throw new IllegalStateException("Retrieval evaluation case has duplicate qrels: " + evalCase.id());
            }
            if (!corpus.containsAll(relevant)) {
                throw new IllegalStateException("Retrieval evaluation case has qrels outside the pinned corpus: " + evalCase.id());
            }
            boolean noMatch = relevant.isEmpty();
            if (noMatch != "no_match".equals(evalCase.queryType())) {
                throw new IllegalStateException("Retrieval evaluation no-match type and qrels disagree: " + evalCase.id());
            }
            if (noMatch && evalCase.expectedArticleNo() != null) {
                throw new IllegalStateException("No-match evaluation case must not declare an expected article: " + evalCase.id());
            }
            if (!noMatch && (evalCase.expectedArticleNo() == null || !relevant.contains(evalCase.expectedArticleNo()))) {
                throw new IllegalStateException("Positive evaluation case primary article must be present in qrels: " + evalCase.id());
            }
        }
    }

    private CaseResult evaluateCase(EvalCase evalCase, List<String> evaluationCorpus) {
        List<KnowledgeMatch> matches = knowledgeMatchingService.searchByQueryBaseline(
            evalCase.query(), topK, evaluationCorpus);
        Set<String> relevantArticleNos = Set.copyOf(evalCase.relevantArticleNos());
        List<RetrievedArticle> retrieved = matches.stream()
            .map(match -> new RetrievedArticle(
                match.article().getArticleNo(),
                match.article().getTitle(),
                match.relevance()
            ))
            .toList();
        List<RelevantRank> relevantRanks = evalCase.relevantArticleNos().stream()
            .map(articleNo -> new RelevantRank(articleNo, rankOf(articleNo, retrieved)))
            .toList();
        int firstRelevantRank = relevantRanks.stream()
            .mapToInt(RelevantRank::rank)
            .filter(rank -> rank > 0)
            .min()
            .orElse(0);
        int expectedRank = evalCase.expectedArticleNo() == null ? 0 : rankOf(evalCase.expectedArticleNo(), retrieved);
        int relevantAt1 = (int) relevantRanks.stream().filter(rank -> rank.rank() == 1).count();
        int relevantAt3 = (int) relevantRanks.stream().filter(rank -> rank.rank() > 0 && rank.rank() <= 3).count();
        boolean noMatch = evalCase.relevantArticleNos().isEmpty();
        boolean hitOne = !noMatch && relevantAt1 > 0;
        boolean hitThree = !noMatch && relevantAt3 > 0;
        boolean correctNoMatch = noMatch && matches.isEmpty();
        double recallAt1 = noMatch ? 0.0 : ratio(relevantAt1, relevantRanks.size());
        double recallAt3 = noMatch ? 0.0 : ratio(relevantAt3, relevantRanks.size());
        double precisionAt3 = noMatch ? 0.0 : (double) relevantAt3 / 3.0;

        String failureType;
        if (noMatch) {
            failureType = correctNoMatch ? "NONE" : "UNEXPECTED_RETRIEVAL";
        } else if (firstRelevantRank == 0) {
            failureType = "NO_RELEVANT_ARTICLE_RETRIEVED";
        } else if (relevantAt3 < relevantRanks.size()) {
            failureType = "PARTIAL_RELEVANT_RECALL";
        } else if (firstRelevantRank > 1) {
            failureType = "RELEVANT_ARTICLE_RANKED_BELOW_FIRST";
        } else {
            failureType = "NONE";
        }

        return new CaseResult(
            evalCase.id(),
            evalCase.query(),
            evalCase.queryType(),
            evalCase.expectedArticleNo(),
            evalCase.relevantArticleNos(),
            expectedRank,
            firstRelevantRank,
            hitOne,
            hitThree,
            correctNoMatch,
            recallAt1,
            recallAt3,
            precisionAt3,
            failureType,
            relevantRanks,
            retrieved
        );
    }

    private static int rankOf(String articleNo, List<RetrievedArticle> retrieved) {
        return IntStream.range(0, retrieved.size())
            .filter(index -> articleNo.equals(retrieved.get(index).articleNo()))
            .map(index -> index + 1)
            .findFirst()
            .orElse(0);
    }

    private static double mean(double[] values) {
        return values.length == 0 ? 0.0 : IntStream.range(0, values.length).mapToDouble(index -> values[index]).average().orElse(0.0);
    }

    private static double ratio(int numerator, int denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }

    public record EvalDataset(
        String version,
        String source,
        String corpusVersion,
        List<String> corpusArticleNos,
        List<EvalCase> cases
    ) {
    }

    public record EvalCase(
        String id,
        String query,
        String queryType,
        String expectedArticleNo,
        List<String> relevantArticleNos,
        String annotationRationale
    ) {
    }

    public record EvaluationReport(
        String generatedAt,
        String datasetVersion,
        String dataSource,
        String corpusVersion,
        int corpusArticleCount,
        String retrievalMode,
        int topK,
        Metrics metrics,
        List<CaseResult> cases
    ) {
    }

    public record Metrics(
        int total,
        int positiveQueryCount,
        int noMatchQueryCount,
        int hitAt1,
        int hitAt3,
        double hitRateAt1,
        double hitRateAt3,
        double recallAt1,
        double recallAt3,
        double precisionAt3,
        double meanReciprocalRank,
        int noMatchTrueNegativeCount,
        int noMatchFalsePositiveCount,
        double noMatchSpecificity
    ) {
    }

    public record CaseResult(
        String id,
        String query,
        String queryType,
        String expectedArticleNo,
        List<String> relevantArticleNos,
        int expectedRank,
        int firstRelevantRank,
        boolean hitAt1,
        boolean hitAt3,
        boolean correctNoMatch,
        double recallAt1,
        double recallAt3,
        double precisionAt3,
        String failureType,
        List<RelevantRank> relevantRanks,
        List<RetrievedArticle> retrieved
    ) {
    }

    public record RelevantRank(String articleNo, int rank) {
    }

    public record RetrievedArticle(String articleNo, String title, int relevance) {
    }
}
