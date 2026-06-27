package com.enterpriseai.ticketcopilot.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.enterpriseai.ticketcopilot.dto.CreateKnowledgeDraftRequest;
import com.enterpriseai.ticketcopilot.dto.CreateTicketRequest;
import com.enterpriseai.ticketcopilot.dto.UpdateTicketStatusRequest;
import com.enterpriseai.ticketcopilot.dto.WorkbenchMetrics;
import com.enterpriseai.ticketcopilot.entity.GenerationRecord;
import com.enterpriseai.ticketcopilot.entity.KnowledgeArticle;
import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import com.enterpriseai.ticketcopilot.entity.TicketAiAnalysisEntity;
import com.enterpriseai.ticketcopilot.entity.TicketStatusHistory;
import com.enterpriseai.ticketcopilot.mapper.GenerationRecordMapper;
import com.enterpriseai.ticketcopilot.mapper.KnowledgeArticleMapper;
import com.enterpriseai.ticketcopilot.mapper.SupportTicketMapper;
import com.enterpriseai.ticketcopilot.mapper.TicketAiAnalysisMapper;
import com.enterpriseai.ticketcopilot.mapper.TicketStatusHistoryMapper;
import com.enterpriseai.ticketcopilot.model.AiAnalysis;
import com.enterpriseai.ticketcopilot.model.KnowledgeDraft;
import com.enterpriseai.ticketcopilot.model.KnowledgeHit;
import com.enterpriseai.ticketcopilot.model.SystemContext;
import com.enterpriseai.ticketcopilot.model.TicketDetail;
import com.enterpriseai.ticketcopilot.model.TicketSummary;
import com.enterpriseai.ticketcopilot.model.TimelineEvent;
import com.enterpriseai.ticketcopilot.model.TraceEvidence;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class TicketWorkflowService {

    public static final String STATUS_PENDING_CLASSIFICATION = "PENDING_CLASSIFICATION";
    public static final String STATUS_PENDING_PROCESS = "PENDING_PROCESS";
    public static final String STATUS_AI_DRAFTED = "AI_DRAFTED";
    public static final String STATUS_REVIEW_REQUIRED = "REVIEW_REQUIRED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_KNOWLEDGE_BASED = "KNOWLEDGE_BASED";
    private static final String CONFIRMATION_STATE = "待人工确认";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TICKET_NO_FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    private final SupportTicketMapper supportTicketMapper;
    private final KnowledgeArticleMapper knowledgeArticleMapper;
    private final TicketAiAnalysisMapper analysisMapper;
    private final TicketStatusHistoryMapper statusHistoryMapper;
    private final GenerationRecordMapper generationRecordMapper;
    private final RuleClassificationService classificationService;
    private final KnowledgeMatchingService knowledgeMatchingService;
    private final RecommendationTemplateService recommendationTemplateService;
    private final AiProviderService aiProviderService;
    private final ObjectMapper objectMapper;

    public TicketWorkflowService(
        SupportTicketMapper supportTicketMapper,
        KnowledgeArticleMapper knowledgeArticleMapper,
        TicketAiAnalysisMapper analysisMapper,
        TicketStatusHistoryMapper statusHistoryMapper,
        GenerationRecordMapper generationRecordMapper,
        RuleClassificationService classificationService,
        KnowledgeMatchingService knowledgeMatchingService,
        RecommendationTemplateService recommendationTemplateService,
        AiProviderService aiProviderService,
        ObjectMapper objectMapper
    ) {
        this.supportTicketMapper = supportTicketMapper;
        this.knowledgeArticleMapper = knowledgeArticleMapper;
        this.analysisMapper = analysisMapper;
        this.statusHistoryMapper = statusHistoryMapper;
        this.generationRecordMapper = generationRecordMapper;
        this.classificationService = classificationService;
        this.knowledgeMatchingService = knowledgeMatchingService;
        this.recommendationTemplateService = recommendationTemplateService;
        this.aiProviderService = aiProviderService;
        this.objectMapper = objectMapper;
    }

    public List<TicketSummary> listTickets() {
        return supportTicketMapper.selectList(new LambdaQueryWrapper<SupportTicket>().orderByDesc(SupportTicket::getUpdatedAt))
            .stream()
            .map(this::toSummary)
            .toList();
    }

    public TicketDetail getTicket(String ticketNo) {
        SupportTicket ticket = findTicket(ticketNo);
        return toDetail(ticket);
    }

    public AiAnalysis getAiAnalysis(String ticketNo) {
        SupportTicket ticket = findTicket(ticketNo);
        TicketAiAnalysisEntity analysis = latestAnalysis(ticket.getId());
        return toAiAnalysis(ticket, analysis);
    }

    public TraceEvidence getTraceEvidence(String ticketNo) {
        SupportTicket ticket = findTicket(ticketNo);
        TicketAiAnalysisEntity analysis = latestAnalysis(ticket.getId());
        List<GenerationRecord> generationRecords = generationRecordMapper.selectList(new LambdaQueryWrapper<GenerationRecord>()
            .eq(GenerationRecord::getBusinessId, ticket.getId())
            .orderByAsc(GenerationRecord::getCreatedAt)
            .orderByAsc(GenerationRecord::getId));
        List<TicketStatusHistory> statusHistories = statusHistoryMapper.selectList(new LambdaQueryWrapper<TicketStatusHistory>()
            .eq(TicketStatusHistory::getTicketId, ticket.getId())
            .orderByAsc(TicketStatusHistory::getOccurredAt)
            .orderByAsc(TicketStatusHistory::getId));
        String runId = "RUN-" + ticket.getTicketNo();
        String traceId = "TRACE-" + ticket.getTicketNo();
        List<TraceEvidence.GenerationRecordEvidence> recordEvidence = generationRecords.stream()
            .map(this::toGenerationEvidence)
            .toList();
        List<TraceEvidence.TraceStep> stepTimeline = generationRecords.stream()
            .map(record -> new TraceEvidence.TraceStep(
                record.getBusinessType(),
                record.getId(),
                record.getSourceType(),
                record.getStatus(),
                record.getLatencyMs(),
                record.getCreatedAt(),
                record.getOutputSummary()
            ))
            .toList();
        List<TraceEvidence.StatusHistoryEvidence> statusEvidence = statusHistories.stream()
            .map(history -> new TraceEvidence.StatusHistoryEvidence(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getActor(),
                history.getNote(),
                history.getOccurredAt()
            ))
            .toList();
        long totalLatency = generationRecords.stream()
            .map(GenerationRecord::getLatencyMs)
            .filter(Objects::nonNull)
            .mapToLong(Long::longValue)
            .sum();
        return new TraceEvidence(
            ticket.getTicketNo(),
            runId,
            traceId,
            "derived-from-ticket-records; no distributed trace/span runtime",
            currentStep(ticket.getStatus()),
            stepTimeline,
            statusEvidence,
            totalLatency,
            reviewRequired(ticket),
            toAiAnalysisEvidence(analysis, generationRecords, totalLatency),
            recordEvidence,
            toRagReferences(ticket, analysis, runId),
            toHumanReviewEvidence(ticket, statusHistories)
        );
    }

    public WorkbenchMetrics metrics() {
        long pending = supportTicketMapper.selectCount(new LambdaQueryWrapper<SupportTicket>()
            .in(SupportTicket::getStatus, STATUS_PENDING_CLASSIFICATION, STATUS_PENDING_PROCESS, STATUS_AI_DRAFTED, STATUS_REVIEW_REQUIRED, STATUS_IN_PROGRESS));
        long total = supportTicketMapper.selectCount(new LambdaQueryWrapper<>());
        Set<Long> ticketsWithKnowledgeMatches = new HashSet<>(analysisMapper.selectList(new LambdaQueryWrapper<TicketAiAnalysisEntity>()
            .isNotNull(TicketAiAnalysisEntity::getMatchedKnowledgeNos)
            .ne(TicketAiAnalysisEntity::getMatchedKnowledgeNos, "")
            .ne(TicketAiAnalysisEntity::getMatchedKnowledgeNos, "[]")).stream()
            .map(TicketAiAnalysisEntity::getTicketId)
            .filter(Objects::nonNull)
            .toList());
        Set<Long> ticketsWithKnowledgeContext = new HashSet<>(ticketsWithKnowledgeMatches);
        knowledgeArticleMapper.selectList(new LambdaQueryWrapper<KnowledgeArticle>()
                .isNotNull(KnowledgeArticle::getSourceTicketId))
            .stream()
            .map(KnowledgeArticle::getSourceTicketId)
            .filter(Objects::nonNull)
            .forEach(ticketsWithKnowledgeContext::add);
        long draftsToday = knowledgeArticleMapper.selectCount(new LambdaQueryWrapper<KnowledgeArticle>()
            .eq(KnowledgeArticle::getStatus, "DRAFT")
            .ge(KnowledgeArticle::getCreatedAt, LocalDate.now().atStartOfDay()));
        int hitRate = percentage(ticketsWithKnowledgeMatches.size(), total);
        int coverage = percentage(ticketsWithKnowledgeContext.size(), total);
        return new WorkbenchMetrics(pending, hitRate, coverage, draftsToday);
    }

    private int percentage(long part, long total) {
        return total == 0 ? 0 : (int) Math.round(part * 100.0 / total);
    }

    @Transactional
    public TicketDetail createTicket(CreateTicketRequest request) {
        LocalDateTime now = LocalDateTime.now();
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketNo(createTicketNo());
        ticket.setTitle(required(request.title(), "title"));
        ticket.setDescription(required(request.description(), "description"));
        ticket.setSystemName(defaultText(request.systemName(), "unknown-system"));
        ticket.setErrorLog(defaultText(request.errorLog(), ""));
        ticket.setUrgency(defaultText(request.urgency(), "P2"));
        ticket.setRequester(defaultText(request.requester(), "员工自助提交"));
        ticket.setRequesterDepartment(defaultText(request.requesterDepartment(), "Internal"));
        ticket.setStatus(STATUS_PENDING_CLASSIFICATION);
        ticket.setCategory("待分类");
        ticket.setAiConfidence(0);
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);
        supportTicketMapper.insert(ticket);
        appendHistory(ticket.getId(), null, STATUS_PENDING_CLASSIFICATION, "System", "工单已提交，进入本地规则分类。");

        long started = System.currentTimeMillis();
        RuleClassificationResult classification = classificationService.classify(ticket.getTitle(), ticket.getDescription(), ticket.getErrorLog());
        saveGeneration(ticket.getId(), "CLASSIFICATION", "RULE_ENGINE", summarize(ticket.getDescription()), classification.category(), started, "SUCCESS");

        started = System.currentTimeMillis();
        List<KnowledgeMatch> matches = knowledgeMatchingService.match(ticket, classification.category());
        saveGeneration(ticket.getId(), "KNOWLEDGE_MATCH", "KEYWORD_MATCHER", classification.category(), "matched=" + matches.size(), started, "SUCCESS");

        started = System.currentTimeMillis();
        RecommendationDraft draft = recommendationTemplateService.generate(ticket, classification.category(), matches);
        TicketAiAnalysisEntity analysis = new TicketAiAnalysisEntity();
        analysis.setTicketId(ticket.getId());
        analysis.setClassification(classification.category());
        analysis.setClassificationReason(classificationReason(ticket, classification, matches));
        analysis.setConfidence(classification.confidence());
        analysis.setConfirmationState(CONFIRMATION_STATE);
        analysis.setMatchedKnowledgeNos(toJson(matches.stream().map(match -> match.article().getArticleNo()).toList()));
        analysis.setTroubleshootingSteps(toJson(draft.troubleshootingSteps()));
        analysis.setReplySuggestion(draft.replySuggestion());
        analysis.setRiskNotes(toJson(draft.riskNotes()));
        analysis.setSourceType("RULE_TEMPLATE");
        analysis.setCreatedAt(LocalDateTime.now());
        analysis.setUpdatedAt(LocalDateTime.now());
        analysisMapper.insert(analysis);
        saveGeneration(ticket.getId(), "RECOMMENDATION", "TEMPLATE_ENGINE", classification.category(), draft.replySuggestion(), started, "SUCCESS");

        ticket.setCategory(classification.category());
        ticket.setAiConfidence(classification.confidence());
        ticket.setStatus(STATUS_PENDING_PROCESS);
        ticket.setUpdatedAt(LocalDateTime.now());
        supportTicketMapper.updateById(ticket);
        appendHistory(ticket.getId(), STATUS_PENDING_CLASSIFICATION, STATUS_PENDING_PROCESS, "Rule Engine", "完成规则分类、知识库匹配和建议草案生成，等待人工确认。");
        return toDetail(ticket);
    }

    @Transactional
    public TicketDetail runCopilot(String ticketNo, String actor) {
        SupportTicket ticket = findTicket(ticketNo);
        long started = System.currentTimeMillis();
        RuleClassificationResult classification = classificationService.classify(ticket.getTitle(), ticket.getDescription(), ticket.getErrorLog());
        saveGeneration(ticket.getId(), "CLASSIFICATION", "RULE_ENGINE", summarize(ticket.getDescription()), classification.category(), started, "SUCCESS");

        started = System.currentTimeMillis();
        List<KnowledgeMatch> matches = knowledgeMatchingService.match(ticket, classification.category());
        saveGeneration(ticket.getId(), "KNOWLEDGE_MATCH", "KEYWORD_MATCHER", classification.category(), "matched=" + matches.size(), started, "SUCCESS");

        RecommendationDraft localDraft = recommendationTemplateService.generate(ticket, classification.category(), matches);
        AiProviderResult providerResult = aiProviderService.complete(ticket, classification.category(), matches, localDraft);
        saveGeneration(ticket.getId(), "AI_PROVIDER", providerResult);

        RecommendationDraft finalDraft = new RecommendationDraft(
            localDraft.troubleshootingSteps(),
            defaultText(providerResult.content(), localDraft.replySuggestion()),
            localDraft.riskNotes()
        );
        saveAnalysis(ticket, classification, matches, finalDraft, providerResult.sourceType());

        String from = ticket.getStatus();
        String target = requiresReview(ticket, finalDraft) ? STATUS_REVIEW_REQUIRED : STATUS_AI_DRAFTED;
        ticket.setCategory(classification.category());
        ticket.setAiConfidence(classification.confidence());
        ticket.setStatus(target);
        ticket.setUpdatedAt(LocalDateTime.now());
        supportTicketMapper.updateById(ticket);
        appendHistory(
            ticket.getId(),
            from,
            target,
            defaultText(actor, "Support Agent"),
            "Copilot 已生成建议草稿，Provider="
                + providerResult.providerName()
                + "，fallback="
                + providerResult.fallbackUsed()
                + (providerResult.fallbackReason() == null ? "" : "，reason=" + providerResult.fallbackReason())
                + "。等待人工审核。"
        );
        return toDetail(ticket);
    }

    @Transactional
    public TicketDetail approveReview(String ticketNo, String actor, String comment) {
        return applyReviewDecision(
            ticketNo,
            STATUS_RESOLVED,
            defaultText(actor, "Reviewer"),
            "APPROVE",
            defaultText(comment, "审核通过，人工确认建议草稿可作为处理结论。")
        );
    }

    @Transactional
    public TicketDetail requestReviewChanges(String ticketNo, String actor, String comment) {
        return applyReviewDecision(
            ticketNo,
            STATUS_REVIEW_REQUIRED,
            defaultText(actor, "Reviewer"),
            "REQUEST_CHANGES",
            defaultText(comment, "需要补充信息后重新审核。")
        );
    }

    @Transactional
    public TicketDetail rejectReview(String ticketNo, String actor, String comment) {
        return applyReviewDecision(
            ticketNo,
            STATUS_REJECTED,
            defaultText(actor, "Reviewer"),
            "REJECT",
            defaultText(comment, "审核拒绝，保留原因并停止当前建议草稿。")
        );
    }

    @Transactional
    public TicketDetail updateStatus(String ticketNo, UpdateTicketStatusRequest request) {
        SupportTicket ticket = findTicket(ticketNo);
        String target = required(request.status(), "status");
        if (!List.of(STATUS_PENDING_PROCESS, STATUS_AI_DRAFTED, STATUS_REVIEW_REQUIRED, STATUS_IN_PROGRESS, STATUS_APPROVED, STATUS_RESOLVED, STATUS_REJECTED, STATUS_KNOWLEDGE_BASED).contains(target)) {
            throw new ResponseStatusException(BAD_REQUEST, "Unsupported status: " + target);
        }
        String from = ticket.getStatus();
        ticket.setStatus(target);
        ticket.setResolvedSummary(defaultText(request.resolvedSummary(), ticket.getResolvedSummary()));
        ticket.setUpdatedAt(LocalDateTime.now());
        supportTicketMapper.updateById(ticket);
        appendHistory(ticket.getId(), from, target, defaultText(request.actor(), "Support Desk"), defaultText(request.note(), "人工确认状态流转。"));
        return toDetail(ticket);
    }

    @Transactional
    public KnowledgeDraft createKnowledgeDraft(String ticketNo, CreateKnowledgeDraftRequest request) {
        SupportTicket ticket = findTicket(ticketNo);
        if (!STATUS_RESOLVED.equals(ticket.getStatus()) && !STATUS_KNOWLEDGE_BASED.equals(ticket.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "Only resolved tickets can generate knowledge drafts.");
        }
        KnowledgeArticle draft = findDraft(ticket.getId());
        if (draft == null) {
            long started = System.currentTimeMillis();
            draft = new KnowledgeArticle();
            draft.setArticleNo("KB-DRAFT-" + LocalDateTime.now().format(TICKET_NO_FORMATTER) + "-" + ThreadLocalRandom.current().nextInt(100, 999));
            draft.setTitle(defaultText(request.title(), "【草稿】" + ticket.getTitle()));
            draft.setCategory(ticket.getCategory());
            draft.setKeywords(ticket.getSystemName() + "," + ticket.getCategory() + "," + ticket.getUrgency());
            draft.setContent(defaultText(request.content(), createDraftContent(ticket)));
            draft.setOwner(defaultText(request.owner(), "Knowledge Owner"));
            draft.setStatus(request.confirm() ? "PUBLISHED" : "DRAFT");
            draft.setSourceTicketId(ticket.getId());
            draft.setCreatedAt(LocalDateTime.now());
            draft.setUpdatedAt(LocalDateTime.now());
            draft.setLastVerifiedAt(request.confirm() ? LocalDateTime.now() : null);
            knowledgeArticleMapper.insert(draft);
            saveGeneration(ticket.getId(), "KNOWLEDGE_DRAFT", "RULE_TEMPLATE", ticket.getTitle(), draft.getTitle(), started, "SUCCESS");
        }
        if (request.confirm() && !"PUBLISHED".equals(draft.getStatus())) {
            publishDraft(ticket, draft);
        }
        return toKnowledgeDraft(draft);
    }

    @Transactional
    public KnowledgeDraft confirmKnowledgeDraft(String articleNo) {
        KnowledgeArticle draft = knowledgeArticleMapper.selectOne(new LambdaQueryWrapper<KnowledgeArticle>()
            .eq(KnowledgeArticle::getArticleNo, articleNo));
        if (draft == null) {
            throw new ResponseStatusException(NOT_FOUND, "Knowledge draft not found: " + articleNo);
        }
        if ("PUBLISHED".equals(draft.getStatus())) {
            return toKnowledgeDraft(draft);
        }
        SupportTicket ticket = supportTicketMapper.selectById(draft.getSourceTicketId());
        if (ticket == null) {
            throw new ResponseStatusException(NOT_FOUND, "Source ticket not found.");
        }
        publishDraft(ticket, draft);
        return toKnowledgeDraft(draft);
    }

    private void publishDraft(SupportTicket ticket, KnowledgeArticle draft) {
        draft.setStatus("PUBLISHED");
        draft.setLastVerifiedAt(LocalDateTime.now());
        draft.setUpdatedAt(LocalDateTime.now());
        knowledgeArticleMapper.updateById(draft);
        String from = ticket.getStatus();
        ticket.setStatus(STATUS_KNOWLEDGE_BASED);
        ticket.setUpdatedAt(LocalDateTime.now());
        supportTicketMapper.updateById(ticket);
        appendHistory(ticket.getId(), from, STATUS_KNOWLEDGE_BASED, "Knowledge Reviewer", "人工确认知识库草稿并完成知识沉淀。");
    }

    private TicketDetail applyReviewDecision(String ticketNo, String targetStatus, String actor, String decision, String comment) {
        SupportTicket ticket = findTicket(ticketNo);
        String from = ticket.getStatus();
        ticket.setStatus(targetStatus);
        if (STATUS_RESOLVED.equals(targetStatus) || STATUS_APPROVED.equals(targetStatus)) {
            ticket.setResolvedSummary(comment);
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        supportTicketMapper.updateById(ticket);
        appendHistory(ticket.getId(), from, targetStatus, actor, decision + ": " + comment);
        return toDetail(ticket);
    }

    private void saveAnalysis(
        SupportTicket ticket,
        RuleClassificationResult classification,
        List<KnowledgeMatch> matches,
        RecommendationDraft draft,
        String sourceType
    ) {
        TicketAiAnalysisEntity analysis = new TicketAiAnalysisEntity();
        analysis.setTicketId(ticket.getId());
        analysis.setClassification(classification.category());
        analysis.setClassificationReason(classificationReason(ticket, classification, matches));
        analysis.setConfidence(classification.confidence());
        analysis.setConfirmationState(CONFIRMATION_STATE);
        analysis.setMatchedKnowledgeNos(toJson(matches.stream().map(match -> match.article().getArticleNo()).toList()));
        analysis.setTroubleshootingSteps(toJson(draft.troubleshootingSteps()));
        analysis.setReplySuggestion(draft.replySuggestion());
        analysis.setRiskNotes(toJson(draft.riskNotes()));
        analysis.setSourceType(defaultText(sourceType, "RULE_TEMPLATE"));
        analysis.setCreatedAt(LocalDateTime.now());
        analysis.setUpdatedAt(LocalDateTime.now());
        analysisMapper.insert(analysis);
    }

    private boolean requiresReview(SupportTicket ticket, RecommendationDraft draft) {
        return "P1".equals(ticket.getUrgency()) || !draft.riskNotes().isEmpty();
    }

    private TicketSummary toSummary(SupportTicket ticket) {
        return new TicketSummary(
            ticket.getTicketNo(),
            ticket.getTitle(),
            ticket.getRequester(),
            ticket.getSystemName(),
            ticket.getStatus(),
            ticket.getUrgency(),
            ticket.getCategory(),
            safeInt(ticket.getAiConfidence()),
            ticket.getUpdatedAt() == null ? "" : ticket.getUpdatedAt().format(TIME_FORMATTER)
        );
    }

    private TicketDetail toDetail(SupportTicket ticket) {
        return new TicketDetail(
            ticket.getTicketNo(),
            ticket.getTitle(),
            ticket.getRequester(),
            ticket.getRequesterDepartment(),
            ticket.getStatus(),
            ticket.getUrgency(),
            ticket.getCategory(),
            ticket.getDescription(),
            new SystemContext(ticket.getSystemName(), "internal", "corp", "-", affectedUsers(ticket.getUrgency())),
            lines(ticket.getErrorLog()),
            List.of("本阶段使用本地规则分类与模板建议，不调用真实 LLM。", "所有处理建议均等待人工确认后执行。"),
            statusHistoryMapper.selectList(new LambdaQueryWrapper<TicketStatusHistory>()
                    .eq(TicketStatusHistory::getTicketId, ticket.getId())
                    .orderByAsc(TicketStatusHistory::getOccurredAt))
                .stream()
                .map(history -> new TimelineEvent(
                    history.getOccurredAt().format(TIME_FORMATTER),
                    history.getToStatus(),
                    history.getActor(),
                    history.getNote()
                ))
                .toList(),
            draftFor(ticket.getId())
        );
    }

    private AiAnalysis toAiAnalysis(SupportTicket ticket, TicketAiAnalysisEntity analysis) {
        List<String> articleNos = fromJson(analysis.getMatchedKnowledgeNos());
        List<KnowledgeHit> hits = articleNos.stream()
            .map(articleNo -> knowledgeArticleMapper.selectOne(new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getArticleNo, articleNo)))
            .filter(article -> article != null)
            .map(article -> new KnowledgeHit(
                article.getArticleNo(),
                article.getTitle(),
                88,
                article.getOwner(),
                article.getLastVerifiedAt() == null ? "-" : article.getLastVerifiedAt().toLocalDate().toString()
            ))
            .toList();
        return new AiAnalysis(
            ticket.getTicketNo(),
            analysis.getClassification(),
            defaultText(
                analysis.getClassificationReason(),
                classificationReason(ticket, new RuleClassificationResult(analysis.getClassification(), safeInt(analysis.getConfidence())), List.of())
            ),
            safeInt(analysis.getConfidence()),
            analysis.getConfirmationState(),
            hits,
            fromJson(analysis.getTroubleshootingSteps()),
            analysis.getReplySuggestion(),
            fromJson(analysis.getRiskNotes())
        );
    }

    private TraceEvidence.AiAnalysisEvidence toAiAnalysisEvidence(TicketAiAnalysisEntity analysis, List<GenerationRecord> records, long totalLatency) {
        GenerationRecord record = analysisRecord(records);
        String status = analysisStatus(records);
        return new TraceEvidence.AiAnalysisEvidence(
            analysis.getId(),
            record == null ? null : record.getId(),
            recordProviderName(record),
            recordModelName(record),
            recordFallbackUsed(record),
            record == null ? "NO_GENERATION_RECORD" : record.getFallbackReason(),
            recordProviderName(record),
            recordModelName(record),
            defaultText(analysis.getSourceType(), record == null ? "RULE_TEMPLATE" : record.getSourceType()),
            totalLatency,
            status,
            analysis.getCreatedAt(),
            record == null ? analysisErrorMessage(status, records) : record.getErrorMessage(),
            record == null ? analysis.getClassificationReason() : record.getInputSummary(),
            defaultText(analysis.getReplySuggestion(), record == null ? "" : record.getOutputSummary())
        );
    }

    private GenerationRecord analysisRecord(List<GenerationRecord> records) {
        GenerationRecord fallback = records.isEmpty() ? null : records.get(records.size() - 1);
        return records.stream()
            .filter(record -> "AI_PROVIDER".equals(record.getBusinessType()))
            .reduce((first, second) -> second)
            .orElseGet(() -> records.stream()
            .filter(record -> "RECOMMENDATION".equals(record.getBusinessType()))
            .reduce((first, second) -> second)
            .orElse(fallback));
    }

    private String analysisStatus(List<GenerationRecord> records) {
        if (records.isEmpty()) {
            return "NO_GENERATION_RECORD";
        }
        GenerationRecord providerRecord = records.stream()
            .filter(record -> "AI_PROVIDER".equals(record.getBusinessType()))
            .reduce((first, second) -> second)
            .orElse(null);
        if (providerRecord != null) {
            return defaultText(providerRecord.getStatus(), "UNKNOWN");
        }
        return records.stream().allMatch(record -> "SUCCESS".equalsIgnoreCase(defaultText(record.getStatus(), ""))) ? "SUCCESS" : "PARTIAL";
    }

    private String analysisErrorMessage(String status, List<GenerationRecord> records) {
        if ("SUCCESS".equals(status)) {
            return null;
        }
        if (records.isEmpty()) {
            return "No generation_record rows were found for this ticket.";
        }
        return records.stream()
            .map(GenerationRecord::getErrorMessage)
            .filter(message -> message != null && !message.isBlank())
            .findFirst()
            .orElse("One or more generation_record rows did not finish successfully.");
    }

    private TraceEvidence.GenerationRecordEvidence toGenerationEvidence(GenerationRecord record) {
        return new TraceEvidence.GenerationRecordEvidence(
            record.getId(),
            record.getBusinessType(),
            record.getSourceType(),
            recordProviderName(record),
            recordModelName(record),
            recordFallbackUsed(record),
            record.getFallbackReason(),
            recordProviderName(record),
            recordModelName(record),
            fallbackStrategy(record.getSourceType()),
            record.getLatencyMs(),
            record.getStatus(),
            record.getCreatedAt(),
            record.getErrorMessage(),
            record.getInputSummary(),
            record.getOutputSummary()
        );
    }

    private String recordProviderName(GenerationRecord record) {
        return record == null ? "local-rule" : defaultText(record.getProviderName(), "local-rule");
    }

    private String recordModelName(GenerationRecord record) {
        return record == null ? "N/A (no LLM)" : defaultText(record.getModelName(), "N/A (no LLM)");
    }

    private boolean recordFallbackUsed(GenerationRecord record) {
        return record == null || Boolean.TRUE.equals(record.getFallbackUsed());
    }

    private List<TraceEvidence.RagReference> toRagReferences(SupportTicket ticket, TicketAiAnalysisEntity analysis, String runId) {
        List<String> articleNos = fromJson(analysis.getMatchedKnowledgeNos());
        Map<String, Integer> relevanceByArticleNo = knowledgeMatchingService.match(ticket, analysis.getClassification()).stream()
            .collect(Collectors.toMap(match -> match.article().getArticleNo(), KnowledgeMatch::relevance, Integer::max));
        return articleNos.stream()
            .map(articleNo -> knowledgeArticleMapper.selectOne(new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getArticleNo, articleNo)))
            .filter(Objects::nonNull)
            .map(article -> new TraceEvidence.RagReference(
                article.getArticleNo(),
                article.getTitle(),
                "knowledge_article/" + article.getArticleNo(),
                matchedKeyword(article, ticket),
                relevanceByArticleNo.get(article.getArticleNo()),
                summarize(article.getContent()),
                articleNos.contains(article.getArticleNo()),
                ticket.getTicketNo(),
                runId
            ))
            .toList();
    }

    private TraceEvidence.HumanReviewEvidence toHumanReviewEvidence(SupportTicket ticket, List<TicketStatusHistory> statusHistories) {
        TicketStatusHistory latestHumanAction = statusHistories.stream()
            .filter(history -> isHumanActor(history.getActor()))
            .reduce((first, second) -> second)
            .orElse(null);
        return new TraceEvidence.HumanReviewEvidence(
            reviewStatus(ticket.getStatus()),
            latestHumanAction == null ? "未分配" : latestHumanAction.getActor(),
            latestHumanAction == null ? "PENDING_REVIEW" : reviewDecision(latestHumanAction.getToStatus()),
            latestHumanAction == null ? "等待人工确认建议草稿、状态流转或知识发布。" : latestHumanAction.getNote(),
            latestHumanAction == null ? null : latestHumanAction.getOccurredAt(),
            nextAction(ticket.getStatus())
        );
    }

    private boolean isHumanActor(String actor) {
        return actor != null && !List.of("System", "Rule Engine", "Rule Template", "系统", "规则引擎", "规则模板").contains(actor);
    }

    private String currentStep(String status) {
        return switch (defaultText(status, "")) {
            case STATUS_PENDING_CLASSIFICATION -> "LOCAL_RULE_CLASSIFICATION";
            case STATUS_PENDING_PROCESS -> "HUMAN_REVIEW_REQUIRED";
            case STATUS_AI_DRAFTED -> "AI_DRAFT_READY";
            case STATUS_REVIEW_REQUIRED -> "HUMAN_REVIEW_REQUIRED";
            case STATUS_IN_PROGRESS -> "HUMAN_PROCESSING";
            case STATUS_APPROVED -> "REVIEW_APPROVED";
            case STATUS_RESOLVED -> "KNOWLEDGE_REVIEW_READY";
            case STATUS_REJECTED -> "REVIEW_REJECTED";
            case STATUS_KNOWLEDGE_BASED -> "KNOWLEDGE_PUBLISHED";
            default -> "UNKNOWN";
        };
    }

    private boolean reviewRequired(SupportTicket ticket) {
        return List.of(STATUS_PENDING_CLASSIFICATION, STATUS_PENDING_PROCESS, STATUS_AI_DRAFTED, STATUS_REVIEW_REQUIRED, STATUS_IN_PROGRESS).contains(ticket.getStatus());
    }

    private String reviewStatus(String status) {
        return switch (defaultText(status, "")) {
            case STATUS_PENDING_PROCESS, STATUS_REVIEW_REQUIRED -> "PENDING";
            case STATUS_AI_DRAFTED -> "AI_DRAFTED";
            case STATUS_IN_PROGRESS -> "IN_PROGRESS";
            case STATUS_APPROVED, STATUS_RESOLVED, STATUS_KNOWLEDGE_BASED -> "COMPLETED";
            case STATUS_REJECTED -> "REJECTED";
            default -> "WAITING_FOR_RULE_ANALYSIS";
        };
    }

    private String reviewDecision(String toStatus) {
        return switch (defaultText(toStatus, "")) {
            case STATUS_IN_PROGRESS -> "TAKE_OWNERSHIP";
            case STATUS_REVIEW_REQUIRED -> "REQUEST_CHANGES";
            case STATUS_APPROVED -> "APPROVED_DRAFT";
            case STATUS_RESOLVED -> "APPROVED_RESOLUTION";
            case STATUS_REJECTED -> "REJECTED";
            case STATUS_KNOWLEDGE_BASED -> "APPROVED_KNOWLEDGE";
            default -> "STATUS_UPDATED";
        };
    }

    private String nextAction(String status) {
        return switch (defaultText(status, "")) {
            case STATUS_PENDING_PROCESS -> "人工确认接手、补充信息或解决状态。";
            case STATUS_AI_DRAFTED -> "审核 AI 建议草稿，决定批准、要求修改或拒绝。";
            case STATUS_REVIEW_REQUIRED -> "高风险或需确认建议必须由 Reviewer 审核。";
            case STATUS_IN_PROGRESS -> "人工复核处理结果，再确认解决或继续排查。";
            case STATUS_APPROVED -> "审核已通过，可由人工继续确认解决。";
            case STATUS_RESOLVED -> "可生成知识草稿，并由知识审核人确认发布。";
            case STATUS_REJECTED -> "建议草稿已拒绝，需要人工重新分析或补充上下文。";
            case STATUS_KNOWLEDGE_BASED -> "已完成知识沉淀；不执行无人值守自动关闭。";
            default -> "等待本地规则分类和模板化建议草稿生成。";
        };
    }

    private String fallbackStrategy(String sourceType) {
        return switch (defaultText(sourceType, "")) {
            case "RULE_ENGINE" -> "keyword rule classification";
            case "KEYWORD_MATCHER" -> "keyword-based RAG reference";
            case "TEMPLATE_ENGINE", "RULE_TEMPLATE" -> "rule template draft";
            case "OPENAI_COMPATIBLE" -> "OpenAI-compatible provider";
            case "LOCAL_RULE_FALLBACK" -> "local-rule fallback";
            default -> "local-rule fallback";
        };
    }

    private String matchedKeyword(KnowledgeArticle article, SupportTicket ticket) {
        String ticketText = normalizeForMatch(ticket.getTitle() + " " + ticket.getDescription() + " " + ticket.getErrorLog() + " " + ticket.getSystemName());
        return Arrays.stream(defaultText(article.getKeywords(), "").split("[,，;；\\s]+"))
            .map(String::trim)
            .filter(keyword -> !keyword.isBlank())
            .filter(keyword -> ticketText.contains(keyword.toLowerCase(Locale.ROOT)))
            .findFirst()
            .orElse("keyword not persisted");
    }

    private String normalizeForMatch(String value) {
        return defaultText(value, "").toLowerCase(Locale.ROOT);
    }

    private TicketAiAnalysisEntity latestAnalysis(Long ticketId) {
        TicketAiAnalysisEntity analysis = analysisMapper.selectOne(new LambdaQueryWrapper<TicketAiAnalysisEntity>()
            .eq(TicketAiAnalysisEntity::getTicketId, ticketId)
            .orderByDesc(TicketAiAnalysisEntity::getCreatedAt)
            .last("limit 1"));
        if (analysis == null) {
            throw new ResponseStatusException(NOT_FOUND, "AI analysis not found.");
        }
        return analysis;
    }

    private KnowledgeDraft draftFor(Long ticketId) {
        KnowledgeArticle draft = findDraft(ticketId);
        return draft == null ? null : toKnowledgeDraft(draft);
    }

    private KnowledgeArticle findDraft(Long ticketId) {
        return knowledgeArticleMapper.selectOne(new LambdaQueryWrapper<KnowledgeArticle>()
            .eq(KnowledgeArticle::getSourceTicketId, ticketId)
            .orderByDesc(KnowledgeArticle::getCreatedAt)
            .last("limit 1"));
    }

    private KnowledgeDraft toKnowledgeDraft(KnowledgeArticle article) {
        return new KnowledgeDraft(article.getArticleNo(), article.getTitle(), article.getCategory(), article.getStatus(), article.getOwner());
    }

    private SupportTicket findTicket(String ticketNo) {
        SupportTicket ticket = supportTicketMapper.selectOne(new LambdaQueryWrapper<SupportTicket>().eq(SupportTicket::getTicketNo, ticketNo));
        if (ticket == null) {
            throw new ResponseStatusException(NOT_FOUND, "Ticket not found: " + ticketNo);
        }
        return ticket;
    }

    private void appendHistory(Long ticketId, String from, String to, String actor, String note) {
        TicketStatusHistory history = new TicketStatusHistory();
        history.setTicketId(ticketId);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setActor(actor);
        history.setNote(note);
        history.setOccurredAt(LocalDateTime.now());
        statusHistoryMapper.insert(history);
    }

    private void saveGeneration(Long businessId, String businessType, String sourceType, String input, String output, long startedAt, String status) {
        saveGeneration(
            businessId,
            businessType,
            sourceType,
            input,
            output,
            System.currentTimeMillis() - startedAt,
            status,
            "local-rule",
            "N/A (no LLM)",
            true,
            "PROVIDER_DISABLED",
            null
        );
    }

    private void saveGeneration(Long businessId, String businessType, AiProviderResult providerResult) {
        saveGeneration(
            businessId,
            businessType,
            providerResult.sourceType(),
            providerResult.promptSummary(),
            providerResult.responseSummary(),
            providerResult.latencyMs(),
            providerResult.status(),
            providerResult.providerName(),
            providerResult.modelName(),
            providerResult.fallbackUsed(),
            providerResult.fallbackReason(),
            providerResult.errorMessage()
        );
    }

    private void saveGeneration(
        Long businessId,
        String businessType,
        String sourceType,
        String input,
        String output,
        long latencyMs,
        String status,
        String providerName,
        String modelName,
        boolean fallbackUsed,
        String fallbackReason,
        String errorMessage
    ) {
        GenerationRecord record = new GenerationRecord();
        record.setBusinessId(businessId);
        record.setBusinessType(businessType);
        record.setSourceType(sourceType);
        record.setProviderName(defaultText(providerName, "local-rule"));
        record.setModelName(defaultText(modelName, "N/A (no LLM)"));
        record.setFallbackUsed(fallbackUsed);
        record.setFallbackReason(fallbackReason);
        record.setErrorMessage(errorMessage);
        record.setInputSummary(summarize(input));
        record.setOutputSummary(summarize(output));
        record.setLatencyMs(latencyMs);
        record.setStatus(status);
        record.setCreatedAt(LocalDateTime.now());
        generationRecordMapper.insert(record);
    }

    private String createDraftContent(SupportTicket ticket) {
        return "来源工单：" + ticket.getTicketNo()
            + "\n分类：" + ticket.getCategory()
            + "\n系统：" + ticket.getSystemName()
            + "\n问题描述：" + ticket.getDescription()
            + "\n解决摘要：" + defaultText(ticket.getResolvedSummary(), "待补充人工解决摘要。")
            + "\n确认要求：该知识条目由规则模板生成，发布前必须人工审核。";
    }

    private String createTicketNo() {
        return "TCK-" + LocalDateTime.now().format(TICKET_NO_FORMATTER) + "-" + ThreadLocalRandom.current().nextInt(100, 999);
    }

    private String classificationReason(SupportTicket ticket, RuleClassificationResult classification, List<KnowledgeMatch> matches) {
        String knowledge = matches.isEmpty() ? "未命中已发布知识库条目" : "命中 " + matches.size() + " 条知识库条目";
        return "本地规则在标题、描述、系统名和错误日志中识别到与「" + classification.category()
            + "」相关的关键词，置信度 " + classification.confidence()
            + "%；" + knowledge + "。该结论仅作为人工确认前的建议。";
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JSON", e);
        }
    }

    private List<String> fromJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private List<String> lines(String value) {
        if (value == null || value.isBlank()) {
            return List.of("暂无错误日志。");
        }
        return value.lines().filter(line -> !line.isBlank()).toList();
    }

    private String affectedUsers(String urgency) {
        return switch (defaultText(urgency, "P2")) {
            case "P1" -> "核心业务或多团队受影响";
            case "P3" -> "单人或低影响范围";
            default -> "局部团队受影响";
        };
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, field + " is required.");
        }
        return value.trim();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String summarize(String value) {
        String normalized = defaultText(value, "");
        return normalized.length() > 180 ? normalized.substring(0, 180) : normalized;
    }
}
