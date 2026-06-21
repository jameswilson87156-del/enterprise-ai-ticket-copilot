package com.enterpriseai.ticketcopilot.service;

import java.util.List;

import com.enterpriseai.ticketcopilot.dto.CreateKnowledgeDraftRequest;
import com.enterpriseai.ticketcopilot.dto.CreateTicketRequest;
import com.enterpriseai.ticketcopilot.dto.UpdateTicketStatusRequest;
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
import com.enterpriseai.ticketcopilot.model.KnowledgeDraft;
import com.enterpriseai.ticketcopilot.model.TicketDetail;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketWorkflowServiceTest {

    @Mock private SupportTicketMapper supportTicketMapper;
    @Mock private KnowledgeArticleMapper knowledgeArticleMapper;
    @Mock private TicketAiAnalysisMapper analysisMapper;
    @Mock private TicketStatusHistoryMapper statusHistoryMapper;
    @Mock private GenerationRecordMapper generationRecordMapper;
    @Mock private RuleClassificationService classificationService;
    @Mock private KnowledgeMatchingService knowledgeMatchingService;
    @Mock private RecommendationTemplateService recommendationTemplateService;

    private TicketWorkflowService service;

    @BeforeEach
    void setUp() {
        service = new TicketWorkflowService(
            supportTicketMapper,
            knowledgeArticleMapper,
            analysisMapper,
            statusHistoryMapper,
            generationRecordMapper,
            classificationService,
            knowledgeMatchingService,
            recommendationTemplateService,
            new ObjectMapper()
        );
    }

    @Test
    void createTicketPersistsAnalysisHistoryAndHumanConfirmationBoundary() {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setArticleNo("KB-API-500");
        article.setTitle("接口 500 排查手册");
        article.setCategory("系统故障");
        article.setOwner("支持知识库");
        KnowledgeMatch match = new KnowledgeMatch(article, 88);

        when(supportTicketMapper.insert(any(SupportTicket.class))).thenAnswer(invocation -> {
            SupportTicket ticket = invocation.getArgument(0);
            ticket.setId(1L);
            return 1;
        });
        when(classificationService.classify(anyString(), anyString(), anyString()))
            .thenReturn(new RuleClassificationResult("系统故障", 92));
        when(knowledgeMatchingService.match(any(SupportTicket.class), anyString())).thenReturn(List.of(match));
        when(recommendationTemplateService.generate(any(SupportTicket.class), anyString(), any()))
            .thenReturn(new RecommendationDraft(List.of("检查 traceId", "人工确认处理动作"), "建议人工排查", List.of("不得自动回滚")));
        when(statusHistoryMapper.selectList(any())).thenReturn(List.of());
        when(knowledgeArticleMapper.selectOne(any())).thenReturn(null);

        TicketDetail detail = service.createTicket(new CreateTicketRequest(
            "报价接口返回 500",
            "销售无法生成报价单",
            "quote-center",
            "HTTP 500",
            "P1",
            "林悦",
            "销售运营"
        ));

        assertThat(detail.status()).isEqualTo(TicketWorkflowService.STATUS_PENDING_PROCESS);
        assertThat(detail.category()).isEqualTo("系统故障");
        assertThat(detail.businessContext()).anyMatch(item -> item.contains("不调用真实 LLM"));
        verify(statusHistoryMapper, times(2)).insert(any(TicketStatusHistory.class));
        verify(generationRecordMapper, times(3)).insert(any(GenerationRecord.class));
        verify(supportTicketMapper).updateById(any(SupportTicket.class));

        ArgumentCaptor<TicketAiAnalysisEntity> analysisCaptor = ArgumentCaptor.forClass(TicketAiAnalysisEntity.class);
        verify(analysisMapper).insert(analysisCaptor.capture());
        assertThat(analysisCaptor.getValue().getConfirmationState()).isEqualTo("待人工确认");
        assertThat(analysisCaptor.getValue().getSourceType()).isEqualTo("RULE_TEMPLATE");
    }

    @Test
    void rejectsUnsupportedStatusWithoutWritingChanges() {
        when(supportTicketMapper.selectOne(any())).thenReturn(ticket(10L, TicketWorkflowService.STATUS_PENDING_PROCESS));

        assertThatThrownBy(() -> service.updateStatus(
            "TCK-1",
            new UpdateTicketStatusRequest("AUTO_CLOSED", "机器人", "自动关闭", "")
        )).isInstanceOfSatisfying(ResponseStatusException.class, exception ->
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
        );

        verify(supportTicketMapper, never()).updateById(any(SupportTicket.class));
        verifyNoInteractions(statusHistoryMapper);
    }

    @Test
    void onlyResolvedTicketsCanGenerateKnowledgeDrafts() {
        when(supportTicketMapper.selectOne(any())).thenReturn(ticket(10L, TicketWorkflowService.STATUS_IN_PROGRESS));

        assertThatThrownBy(() -> service.createKnowledgeDraft(
            "TCK-1",
            new CreateKnowledgeDraftRequest(null, null, "知识审核人", false)
        )).isInstanceOfSatisfying(ResponseStatusException.class, exception ->
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
        );

        verify(knowledgeArticleMapper, never()).insert(any(KnowledgeArticle.class));
    }

    @Test
    void confirmingDraftPublishesKnowledgeAndUpdatesTicket() {
        KnowledgeArticle draft = draft("DRAFT");
        SupportTicket ticket = ticket(10L, TicketWorkflowService.STATUS_RESOLVED);
        when(knowledgeArticleMapper.selectOne(any())).thenReturn(draft);
        when(supportTicketMapper.selectById(10L)).thenReturn(ticket);

        KnowledgeDraft result = service.confirmKnowledgeDraft(draft.getArticleNo());

        assertThat(result.status()).isEqualTo("PUBLISHED");
        assertThat(draft.getLastVerifiedAt()).isNotNull();
        assertThat(ticket.getStatus()).isEqualTo(TicketWorkflowService.STATUS_KNOWLEDGE_BASED);
        verify(knowledgeArticleMapper).updateById(draft);
        verify(supportTicketMapper).updateById(ticket);
        verify(statusHistoryMapper).insert(any(TicketStatusHistory.class));
    }

    @Test
    void confirmingPublishedDraftIsIdempotent() {
        KnowledgeArticle published = draft("PUBLISHED");
        when(knowledgeArticleMapper.selectOne(any())).thenReturn(published);

        KnowledgeDraft result = service.confirmKnowledgeDraft(published.getArticleNo());

        assertThat(result.status()).isEqualTo("PUBLISHED");
        verify(knowledgeArticleMapper, never()).updateById(any(KnowledgeArticle.class));
        verifyNoInteractions(supportTicketMapper, statusHistoryMapper);
    }

    private SupportTicket ticket(Long id, String status) {
        SupportTicket ticket = new SupportTicket();
        ticket.setId(id);
        ticket.setTicketNo("TCK-1");
        ticket.setTitle("测试工单");
        ticket.setDescription("测试描述");
        ticket.setSystemName("demo-system");
        ticket.setErrorLog("");
        ticket.setUrgency("P2");
        ticket.setRequester("测试人员");
        ticket.setRequesterDepartment("内部支持");
        ticket.setStatus(status);
        ticket.setCategory("系统故障");
        ticket.setAiConfidence(88);
        return ticket;
    }

    private KnowledgeArticle draft(String status) {
        KnowledgeArticle draft = new KnowledgeArticle();
        draft.setId(20L);
        draft.setArticleNo("KB-DRAFT-1");
        draft.setTitle("测试知识草稿");
        draft.setCategory("系统故障");
        draft.setOwner("知识审核人");
        draft.setStatus(status);
        draft.setSourceTicketId(10L);
        return draft;
    }
}
