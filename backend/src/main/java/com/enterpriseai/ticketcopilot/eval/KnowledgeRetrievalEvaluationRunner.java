package com.enterpriseai.ticketcopilot.eval;

import java.util.Locale;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Optional command-line entry point for the same evaluation exposed by the API. */
@Component
@ConditionalOnProperty(
    prefix = "ticket-copilot.evaluation.retrieval",
    name = "enabled",
    havingValue = "true"
)
public class KnowledgeRetrievalEvaluationRunner implements ApplicationRunner {

    private final RetrievalEvaluationService evaluationService;

    public KnowledgeRetrievalEvaluationRunner(RetrievalEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var report = evaluationService.evaluateAndSave();
        System.out.printf(Locale.ROOT,
            "检索评估完成：dataset=%s, corpus=%d, cases=%d, positive=%d, noMatch=%d, hitRate@1=%.4f, macroRecall@3=%.4f, mrr=%.4f, noMatchSpecificity=%.4f%n",
            report.datasetVersion(),
            report.corpusArticleCount(),
            report.metrics().total(),
            report.metrics().positiveQueryCount(),
            report.metrics().noMatchQueryCount(),
            report.metrics().hitRateAt1(),
            report.metrics().recallAt3(),
            report.metrics().meanReciprocalRank(),
            report.metrics().noMatchSpecificity()
        );
    }
}
