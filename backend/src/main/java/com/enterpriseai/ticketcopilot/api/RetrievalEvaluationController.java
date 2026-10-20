package com.enterpriseai.ticketcopilot.api;

import com.enterpriseai.ticketcopilot.eval.RetrievalEvaluationService;
import com.enterpriseai.ticketcopilot.eval.RetrievalEvaluationService.EvaluationReport;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/evaluations/retrieval", produces = "application/json;charset=UTF-8")
public class RetrievalEvaluationController {

    private final RetrievalEvaluationService evaluationService;

    public RetrievalEvaluationController(RetrievalEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/run")
    public EvaluationReport runEvaluation() throws Exception {
        return evaluationService.evaluateAndSave();
    }
}
