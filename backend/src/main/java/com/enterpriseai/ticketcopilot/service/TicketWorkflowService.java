package com.enterpriseai.ticketcopilot.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

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
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_RESOLVED = "RESOLVED";
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

    public WorkbenchMetrics metrics() {
        long pending = supportTicketMapper.selectCount(new LambdaQueryWrapper<SupportTicket>()
            .in(SupportTicket::getStatus, STATUS_PENDING_CLASSIFICATION, STATUS_PENDING_PROCESS, STATUS_IN_PROGRESS));
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
    public TicketDetail updateStatus(String ticketNo, UpdateTicketStatusRequest request) {
        SupportTicket ticket = findTicket(ticketNo);
        String target = required(request.status(), "status");
        if (!List.of(STATUS_PENDING_PROCESS, STATUS_IN_PROGRESS, STATUS_RESOLVED, STATUS_KNOWLEDGE_BASED).contains(target)) {
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
        GenerationRecord record = new GenerationRecord();
        record.setBusinessId(businessId);
        record.setBusinessType(businessType);
        record.setSourceType(sourceType);
        record.setInputSummary(summarize(input));
        record.setOutputSummary(summarize(output));
        record.setLatencyMs(System.currentTimeMillis() - startedAt);
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
