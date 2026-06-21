import type {
  AiAnalysis,
  CreateTicketRequest,
  KnowledgeDraft,
  TicketDetail,
  TicketStatus,
  TicketSummary,
  WorkbenchMetrics
} from '../types/ticket'

type DemoTicketSeed = {
  summary: TicketSummary
  detail: TicketDetail
  analysis: AiAnalysis
}

const statusLabel: Record<TicketStatus, string> = {
  PENDING_CLASSIFICATION: '待分类',
  PENDING_PROCESS: '待人工确认',
  IN_PROGRESS: '处理中',
  RESOLVED: '已解决',
  KNOWLEDGE_BASED: '已沉淀'
}

const clone = <T>(value: T): T => JSON.parse(JSON.stringify(value)) as T

const wait = async <T>(value: T): Promise<T> => {
  await new Promise((resolve) => window.setTimeout(resolve, 120))
  return clone(value)
}

const seeds: DemoTicketSeed[] = [
  {
    summary: {
      id: 'DEMO-0005',
      title: '报价接口返回 500，销售无法生成报价单',
      requester: '林悦',
      team: 'quote-center',
      status: 'IN_PROGRESS',
      priority: 'P1',
      category: '系统故障',
      aiConfidence: 92,
      updatedAt: '11:10'
    },
    detail: {
      id: 'DEMO-0005',
      title: '报价接口返回 500，销售无法生成报价单',
      requester: '林悦',
      department: '销售运营',
      status: 'IN_PROGRESS',
      priority: 'P1',
      category: '系统故障',
      description: '销售在报价中心提交企业客户报价时接口返回 500，错误集中在华东区域，影响本日上午的报价单生成。',
      systemContext: {
        application: 'quote-center',
        environment: 'internal',
        region: '华东业务区',
        lastDeployment: '今天 09:40 灰度发布',
        affectedUsers: '销售运营与客户成功团队'
      },
      errorLogs: ['HTTP 500 traceId=qc-20260620-500', 'NullPointerException at PricePolicyService.resolvePolicy'],
      businessContext: ['P1 工单，影响报价链路和客户跟进时效。', '规则建议只作为排查草案，降级、回滚或补数据必须由人工确认。'],
      timeline: [
        { time: '10:24', state: 'PENDING_CLASSIFICATION', actor: '系统', note: '工单进入规则分类队列。' },
        { time: '10:29', state: 'PENDING_PROCESS', actor: '规则引擎', note: '完成分类、知识命中和建议草案生成。' },
        { time: '10:44', state: 'IN_PROGRESS', actor: '支持人员', note: '支持人员人工接手，关联 traceId 排查。' }
      ],
      knowledgeDraft: null
    },
    analysis: {
      ticketId: 'DEMO-0005',
      classification: '系统故障',
      classificationReason: '本地规则识别到接口 500、traceId、NullPointerException 等关键词，并命中接口 500 排查手册；结论等待人工确认。',
      confidence: 92,
      confirmationState: '待人工确认',
      knowledgeHits: [
        {
          id: 'KB-API-500',
          title: '接口 500 错误分层排查手册',
          relevance: 88,
          owner: '支持知识库',
          lastVerifiedAt: '2026-06-20'
        },
        {
          id: 'KB-OPS-003',
          title: '系统故障超时与 5xx 排查手册',
          relevance: 74,
          owner: '运维知识库',
          lastVerifiedAt: '2026-06-19'
        }
      ],
      troubleshootingSteps: [
        '使用 traceId 关联网关、应用日志和依赖调用。',
        '确认 PricePolicyService 空指针输入来源和最近发布差异。',
        '由值班人员确认修复、降级或回滚策略。'
      ],
      replySuggestion:
        '报价接口 500 已进入人工排查，请保留失败报价单和 traceId。支持人员确认处理动作后，会同步修复进度与临时方案。',
      riskNotes: ['报价链路影响销售收入，降级或回滚必须人工确认。', '不得自动修改生产配置或批量重放报价请求。']
    }
  },
  {
    summary: {
      id: 'DEMO-0003',
      title: 'Redis 连接失败导致会话校验超时',
      requester: '唐宁',
      team: 'employee-portal',
      status: 'IN_PROGRESS',
      priority: 'P1',
      category: '系统故障',
      aiConfidence: 90,
      updatedAt: '10:30'
    },
    detail: {
      id: 'DEMO-0003',
      title: 'Redis 连接失败导致会话校验超时',
      requester: '唐宁',
      department: '员工体验',
      status: 'IN_PROGRESS',
      priority: 'P1',
      category: '系统故障',
      description: '员工门户登录后频繁掉线，应用日志显示 Redis timeout，影响多个部门的自助流程。',
      systemContext: {
        application: 'employee-portal',
        environment: 'internal',
        region: 'corp',
        lastDeployment: '昨天 18:30',
        affectedUsers: '多个内部部门'
      },
      errorLogs: ['io.lettuce.core.RedisConnectionException: Unable to connect to redis-prod:6379'],
      businessContext: ['会话链路涉及员工门户登录稳定性。', '连接池参数和网络 ACL 变更需要人工确认。'],
      timeline: [
        { time: '09:40', state: 'PENDING_CLASSIFICATION', actor: '系统', note: '工单提交，进入规则分类。' },
        { time: '09:45', state: 'PENDING_PROCESS', actor: '规则引擎', note: '命中 Redis 连接失败知识条目。' },
        { time: '10:05', state: 'IN_PROGRESS', actor: '支持人员', note: '支持人员接手并联系平台值班。' }
      ],
      knowledgeDraft: null
    },
    analysis: {
      ticketId: 'DEMO-0003',
      classification: '系统故障',
      classificationReason: '本地规则识别到 Redis、连接失败、timeout 等关键词，并命中 Redis 连接池排查知识。',
      confidence: 90,
      confirmationState: '待人工确认',
      knowledgeHits: [{ id: 'KB-REDIS-CONN', title: 'Redis 连接失败与连接池排查', relevance: 88, owner: '基础平台', lastVerifiedAt: '2026-06-20' }],
      troubleshootingSteps: ['检查 Redis 地址、密码和网络 ACL。', '查看连接池使用率和超时配置。', '确认是否需要临时扩容或切换备用节点。'],
      replySuggestion: '初步判断为 Redis 连接异常，请值班人员确认网络和连接池状态后处理。',
      riskNotes: ['不能自动修改 Redis 生产参数。', '会话链路影响面需要人工评估。']
    }
  },
  {
    summary: {
      id: 'DEMO-0002',
      title: 'Spring Boot 启动时报 BeanCreationException',
      requester: '周冉',
      team: 'payment-service',
      status: 'PENDING_PROCESS',
      priority: 'P1',
      category: '系统故障',
      aiConfidence: 88,
      updatedAt: '10:00'
    },
    detail: {
      id: 'DEMO-0002',
      title: 'Spring Boot 启动时报 BeanCreationException',
      requester: '周冉',
      department: '支付研发',
      status: 'PENDING_PROCESS',
      priority: 'P1',
      category: '系统故障',
      description: 'payment-service 发布后启动失败，怀疑新增结算策略 bean 缺少配置。',
      systemContext: {
        application: 'payment-service',
        environment: 'UAT',
        region: 'corp',
        lastDeployment: '今天 09:15',
        affectedUsers: '支付研发发布验证'
      },
      errorLogs: ['BeanCreationException: Error creating bean with name settlementStrategy'],
      businessContext: ['发布验证阻塞，需要研发确认配置差异。', '回滚或补配置均需人工确认。'],
      timeline: [
        { time: '09:14', state: 'PENDING_CLASSIFICATION', actor: '系统', note: '工单提交，进入本地规则分类。' },
        { time: '09:19', state: 'PENDING_PROCESS', actor: '规则引擎', note: '生成启动失败排查建议，等待人工确认。' }
      ],
      knowledgeDraft: null
    },
    analysis: {
      ticketId: 'DEMO-0002',
      classification: '系统故障',
      classificationReason: '本地规则识别到 Spring Boot、BeanCreationException、bean 创建失败等关键词，并命中启动失败处理清单。',
      confidence: 88,
      confirmationState: '待人工确认',
      knowledgeHits: [
        { id: 'KB-SPRING-BEAN', title: 'Spring Boot BeanCreationException 处理清单', relevance: 88, owner: 'Java 平台组', lastVerifiedAt: '2026-06-20' }
      ],
      troubleshootingSteps: ['定位异常 bean 和缺失配置项。', '检查 profile、配置中心和构造器依赖。', '由研发确认补配置或回滚版本。'],
      replySuggestion: '建议先核对 settlementStrategy 相关配置和 profile，确认后再补齐配置或回滚。',
      riskNotes: ['配置变更需人工确认。', '回滚会影响当前发布窗口。']
    }
  },
  {
    summary: {
      id: 'DEMO-0004',
      title: 'CRM 客户列表查询超过 12 秒',
      requester: '韩岚',
      team: 'crm-analytics',
      status: 'PENDING_PROCESS',
      priority: 'P2',
      category: '数据问题',
      aiConfidence: 84,
      updatedAt: '09:50'
    },
    detail: {
      id: 'DEMO-0004',
      title: 'CRM 客户列表查询超过 12 秒',
      requester: '韩岚',
      department: '客户成功',
      status: 'PENDING_PROCESS',
      priority: 'P2',
      category: '数据问题',
      description: '客户成功团队反馈 CRM 客户列表打开很慢，筛选续费状态后等待时间明显变长。',
      systemContext: {
        application: 'crm-analytics',
        environment: 'internal',
        region: 'corp',
        lastDeployment: '昨天 21:10',
        affectedUsers: '客户成功团队'
      },
      errorLogs: ['slow sql: select * from customer_account where renewal_status = ? order by updated_at desc'],
      businessContext: ['影响客户经理日常跟进效率。', '索引调整需 DBA 评估写入影响。'],
      timeline: [
        { time: '08:55', state: 'PENDING_CLASSIFICATION', actor: '系统', note: '工单进入规则分类。' },
        { time: '09:00', state: 'PENDING_PROCESS', actor: '规则引擎', note: '命中慢查询知识条目，等待人工确认。' }
      ],
      knowledgeDraft: null
    },
    analysis: {
      ticketId: 'DEMO-0004',
      classification: '数据问题',
      classificationReason: '本地规则识别到 MySQL、慢查询、SQL、执行计划等关键词，并命中慢查询处理流程。',
      confidence: 84,
      confirmationState: '待人工确认',
      knowledgeHits: [
        { id: 'KB-MYSQL-SLOW', title: 'MySQL 慢查询定位与索引评估流程', relevance: 88, owner: 'DBA 团队', lastVerifiedAt: '2026-06-20' }
      ],
      troubleshootingSteps: ['收集慢查询 SQL 和执行计划。', '核对 renewal_status、updated_at 索引命中。', '由 DBA 评估新增索引或 SQL 改写。'],
      replySuggestion: '建议先由 DBA 查看执行计划和索引命中情况，再确认优化方案。',
      riskNotes: ['新增索引会影响写入性能。', 'SQL 改写需回归核心筛选场景。']
    }
  },
  {
    summary: {
      id: 'DEMO-0001',
      title: 'Java 服务启动失败，提示 8080 端口已被占用',
      requester: '李航',
      team: 'order-service',
      status: 'IN_PROGRESS',
      priority: 'P2',
      category: '系统故障',
      aiConfidence: 91,
      updatedAt: '09:10'
    },
    detail: {
      id: 'DEMO-0001',
      title: 'Java 服务启动失败，提示 8080 端口已被占用',
      requester: '李航',
      department: '研发效能',
      status: 'IN_PROGRESS',
      priority: 'P2',
      category: '系统故障',
      description: '发布 order-service 后新实例无法启动，启动日志显示端口绑定失败，影响测试环境验收。',
      systemContext: {
        application: 'order-service',
        environment: 'test',
        region: 'corp',
        lastDeployment: '今天 08:30',
        affectedUsers: '研发测试团队'
      },
      errorLogs: ['java.net.BindException: Address already in use: bind 0.0.0.0:8080'],
      businessContext: ['测试环境验收受阻。', '释放端口或调整配置前需人工确认。'],
      timeline: [
        { time: '08:12', state: 'PENDING_CLASSIFICATION', actor: '系统', note: '工单提交，等待规则分类。' },
        { time: '08:17', state: 'PENDING_PROCESS', actor: '规则引擎', note: '命中端口占用知识库。' },
        { time: '08:34', state: 'IN_PROGRESS', actor: '支持人员', note: '支持人员确认旧实例占用。' }
      ],
      knowledgeDraft: null
    },
    analysis: {
      ticketId: 'DEMO-0001',
      classification: '系统故障',
      classificationReason: '本地规则识别到 Java、端口、BindException、8080 等关键词，并命中端口占用知识库。',
      confidence: 91,
      confirmationState: '待人工确认',
      knowledgeHits: [{ id: 'KB-JAVA-PORT', title: 'Java 服务端口占用排查手册', relevance: 88, owner: '运维知识库', lastVerifiedAt: '2026-06-20' }],
      troubleshootingSteps: ['确认 8080 端口监听进程和启动时间。', '核对是否存在旧实例未退出或重复启动。', '由值班人员确认释放端口或调整 server.port。'],
      replySuggestion: '已识别为 Java 服务端口占用导致启动失败，请支持人员确认占用进程后再执行释放或配置调整。',
      riskNotes: ['不得自动 kill 生产进程。', '端口调整需同步调用方和部署配置。']
    }
  },
  {
    summary: {
      id: 'DEMO-0006',
      title: '新员工无法访问财务共享盘',
      requester: '周明',
      team: 'corp-file-service',
      status: 'PENDING_PROCESS',
      priority: 'P3',
      category: '权限问题',
      aiConfidence: 86,
      updatedAt: '08:55'
    },
    detail: {
      id: 'DEMO-0006',
      title: '新员工无法访问财务共享盘',
      requester: '周明',
      department: '财务部',
      status: 'PENDING_PROCESS',
      priority: 'P3',
      category: '权限问题',
      description: '新员工入职后访问财务共享盘提示 403，主管已完成审批。',
      systemContext: {
        application: 'corp-file-service',
        environment: 'internal',
        region: 'corp',
        lastDeployment: '本周无发布',
        affectedUsers: '单个员工'
      },
      errorLogs: ['403 Forbidden user not in finance-share-rw group'],
      businessContext: ['涉及财务目录访问权限。', '授权必须保留审批和审计记录。'],
      timeline: [
        { time: '08:10', state: 'PENDING_CLASSIFICATION', actor: '系统', note: '进入规则分类。' },
        { time: '08:15', state: 'PENDING_PROCESS', actor: '规则引擎', note: '命中 RBAC 核验流程，等待人工确认。' }
      ],
      knowledgeDraft: null
    },
    analysis: {
      ticketId: 'DEMO-0006',
      classification: '权限问题',
      classificationReason: '本地规则识别到 403、权限、共享盘、审批等关键词，并命中 RBAC 核验流程。',
      confidence: 86,
      confirmationState: '待人工确认',
      knowledgeHits: [{ id: 'KB-IAM-ROLE', title: '权限配置问题与 RBAC 核验流程', relevance: 88, owner: 'IT 治理', lastVerifiedAt: '2026-06-20' }],
      troubleshootingSteps: ['核验审批记录和直属主管确认。', '检查用户是否在 finance-share-rw 组。', '人工确认后执行授权并记录审计。'],
      replySuggestion: '该问题疑似权限组未同步，需人工核验审批记录后处理。',
      riskNotes: ['涉及财务目录，不能自动授权。', '必须保留审批与审计记录。']
    }
  },
  {
    summary: {
      id: 'DEMO-0007',
      title: '部署后缺少 PAYMENT_GATEWAY_URL 环境变量',
      requester: '赵琳',
      team: 'payment-service',
      status: 'RESOLVED',
      priority: 'P2',
      category: '系统故障',
      aiConfidence: 89,
      updatedAt: '08:35'
    },
    detail: {
      id: 'DEMO-0007',
      title: '部署后缺少 PAYMENT_GATEWAY_URL 环境变量',
      requester: '赵琳',
      department: '发布工程',
      status: 'RESOLVED',
      priority: 'P2',
      category: '系统故障',
      description: 'UAT 环境部署 payment-service 后健康检查失败，日志显示缺少支付网关 URL。',
      systemContext: {
        application: 'payment-service',
        environment: 'UAT',
        region: 'corp',
        lastDeployment: '今天 08:00',
        affectedUsers: '发布验证团队'
      },
      errorLogs: ['IllegalStateException: Missing required environment variable PAYMENT_GATEWAY_URL'],
      businessContext: ['已由发布同学补齐 UAT 环境变量并完成人工验证。', '可以生成知识草稿，但发布前仍需人工审核。'],
      timeline: [
        { time: '08:00', state: 'PENDING_CLASSIFICATION', actor: '系统', note: '进入规则分类。' },
        { time: '08:05', state: 'PENDING_PROCESS', actor: '规则引擎', note: '命中部署配置知识。' },
        { time: '08:20', state: 'IN_PROGRESS', actor: '支持人员', note: '发布负责人接手处理。' },
        { time: '08:30', state: 'RESOLVED', actor: '支持人员', note: '已人工确认解决，等待知识沉淀。' }
      ],
      knowledgeDraft: {
        articleNo: 'KB-DRAFT-DEMO-0007',
        title: '【草稿】部署环境变量缺失排查清单',
        category: '系统故障',
        status: 'DRAFT',
        owner: '知识审核人'
      }
    },
    analysis: {
      ticketId: 'DEMO-0007',
      classification: '系统故障',
      classificationReason: '本地规则识别到部署、环境变量、Missing required environment variable 等关键词，并命中部署配置知识。',
      confidence: 89,
      confirmationState: '待人工确认',
      knowledgeHits: [{ id: 'KB-DEPLOY-ENV', title: '部署环境变量缺失排查清单', relevance: 88, owner: '发布工程', lastVerifiedAt: '2026-06-20' }],
      troubleshootingSteps: ['比对 UAT 与生产环境变量差异。', '检查 ConfigMap、Secret 和启动参数。', '人工确认补齐变量并重启验证。'],
      replySuggestion: '建议由发布负责人确认 PAYMENT_GATEWAY_URL 来源后补齐 UAT 配置。',
      riskNotes: ['部署配置变更需人工确认。', '避免把生产密钥误写入 UAT。']
    }
  },
  {
    summary: {
      id: 'DEMO-0008',
      title: '多团队重复咨询采购系统审批流程',
      requester: '王可',
      team: 'procurement-flow',
      status: 'KNOWLEDGE_BASED',
      priority: 'P3',
      category: '流程咨询',
      aiConfidence: 82,
      updatedAt: '08:20'
    },
    detail: {
      id: 'DEMO-0008',
      title: '多团队重复咨询采购系统审批流程',
      requester: '王可',
      department: '业务支持',
      status: 'KNOWLEDGE_BASED',
      priority: 'P3',
      category: '流程咨询',
      description: '近两周多个部门反复咨询采购系统审批链路和材料要求，建议沉淀标准 FAQ。',
      systemContext: {
        application: 'procurement-flow',
        environment: 'internal',
        region: 'corp',
        lastDeployment: '本周无发布',
        affectedUsers: '多部门流程咨询'
      },
      errorLogs: ['no runtime error; repeated process consultation'],
      businessContext: ['重复咨询适合沉淀知识条目。', '知识发布前需要业务负责人和知识负责人确认。'],
      timeline: [
        { time: '07:30', state: 'PENDING_CLASSIFICATION', actor: '系统', note: '工单进入规则分类。' },
        { time: '07:35', state: 'PENDING_PROCESS', actor: '规则引擎', note: '命中知识沉淀规范。' },
        { time: '07:50', state: 'IN_PROGRESS', actor: '支持人员', note: '归纳重复问题与标准答复。' },
        { time: '08:05', state: 'RESOLVED', actor: '支持人员', note: '人工确认流程口径。' },
        { time: '08:20', state: 'KNOWLEDGE_BASED', actor: '知识审核人', note: '知识草稿已人工确认并发布。' }
      ],
      knowledgeDraft: {
        articleNo: 'KB-DRAFT-DEMO-0008',
        title: '采购系统审批流程 FAQ',
        category: '流程咨询',
        status: 'PUBLISHED',
        owner: '知识审核人'
      }
    },
    analysis: {
      ticketId: 'DEMO-0008',
      classification: '流程咨询',
      classificationReason: '本地规则识别到重复咨询、流程、FAQ、知识库等关键词，并命中知识沉淀规范。',
      confidence: 82,
      confirmationState: '待人工确认',
      knowledgeHits: [{ id: 'KB-FAQ-KNOWLEDGE', title: '重复咨询转知识库沉淀规范', relevance: 88, owner: '知识运营', lastVerifiedAt: '2026-06-20' }],
      troubleshootingSteps: ['归纳重复问题和标准答复。', '生成知识草稿并指定知识负责人审核。', '人工确认后发布并关联原工单。'],
      replySuggestion: '建议将采购审批流程整理为知识库 FAQ，由知识负责人确认后发布。',
      riskNotes: ['知识发布前必须人工审核。', '流程条款需要业务负责人确认。']
    }
  }
]

let tickets = seeds.map((seed) => clone(seed.summary))
let ticketDetails = Object.fromEntries(seeds.map((seed) => [seed.detail.id, clone(seed.detail)])) as Record<string, TicketDetail>
let analyses = Object.fromEntries(seeds.map((seed) => [seed.analysis.ticketId, clone(seed.analysis)])) as Record<string, AiAnalysis>

function ensureTicket(id: string) {
  const detail = ticketDetails[id]
  if (!detail) {
    throw new Error(`Demo ticket not found: ${id}`)
  }
  return detail
}

function syncSummary(detail: TicketDetail) {
  const target = tickets.find((ticket) => ticket.id === detail.id)
  if (!target) {
    return
  }
  target.status = detail.status
  target.category = detail.category
  target.updatedAt = '刚刚'
}

function buildSearchText(ticket: TicketSummary) {
  const detail = ticketDetails[ticket.id]
  const analysis = analyses[ticket.id]
  return [
    detail?.description,
    ...(detail?.errorLogs ?? []),
    ...(detail?.businessContext ?? []),
    analysis?.classificationReason,
    ...(analysis?.knowledgeHits.flatMap((hit) => [hit.id, hit.title, hit.owner]) ?? []),
    ...(analysis?.troubleshootingSteps ?? [])
  ]
    .filter(Boolean)
    .join(' ')
}

function classify(payload: CreateTicketRequest) {
  const text = `${payload.title} ${payload.description} ${payload.errorLog}`.toLowerCase()
  if (text.includes('权限') || text.includes('403') || text.includes('forbidden')) {
    return { category: '权限问题', confidence: 86 }
  }
  if (text.includes('数据') || text.includes('报表') || text.includes('sql')) {
    return { category: '数据问题', confidence: 84 }
  }
  if (text.includes('流程') || text.includes('咨询') || text.includes('faq')) {
    return { category: '流程咨询', confidence: 80 }
  }
  return { category: '系统故障', confidence: 88 }
}

export function demoFetchTickets() {
  return wait(tickets.map((ticket) => ({ ...ticket, searchText: buildSearchText(ticket) })))
}

export function demoFetchTicket(id: string) {
  return wait(ensureTicket(id))
}

export function demoFetchAiAnalysis(id: string) {
  return wait(analyses[id])
}

export function demoFetchMetrics(): Promise<WorkbenchMetrics> {
  const pendingTickets = tickets.filter((ticket) => !['RESOLVED', 'KNOWLEDGE_BASED'].includes(ticket.status)).length
  const aiHitRate = Math.round((tickets.filter((ticket) => ticket.aiConfidence > 0).length / tickets.length) * 100)
  const ticketsWithKnowledgeContext = new Set<string>()
  Object.values(analyses).forEach((analysis) => {
    if (analysis.knowledgeHits.length > 0) {
      ticketsWithKnowledgeContext.add(analysis.ticketId)
    }
  })
  Object.values(ticketDetails).forEach((ticket) => {
    if (ticket.knowledgeDraft) {
      ticketsWithKnowledgeContext.add(ticket.id)
    }
  })
  const knowledgeCoverage = tickets.length === 0 ? 0 : Math.round((ticketsWithKnowledgeContext.size / tickets.length) * 100)
  const todayKnowledgeDrafts = Object.values(ticketDetails).filter((ticket) => ticket.knowledgeDraft?.status === 'DRAFT').length
  return wait({
    pendingTickets,
    aiHitRate,
    knowledgeCoverage,
    todayKnowledgeDrafts
  })
}

export function demoCreateTicket(payload: CreateTicketRequest) {
  const { category, confidence } = classify(payload)
  const id = `DEMO-NEW-${String(tickets.length + 1).padStart(2, '0')}`
  const summary: TicketSummary = {
    id,
    title: payload.title,
    requester: payload.requester || '员工自助提交',
    team: payload.systemName,
    status: 'PENDING_PROCESS',
    priority: payload.urgency,
    category,
    aiConfidence: confidence,
    updatedAt: '刚刚'
  }
  const detail: TicketDetail = {
    id,
    title: payload.title,
    requester: payload.requester || '员工自助提交',
    department: payload.requesterDepartment || '内部支持',
    status: 'PENDING_PROCESS',
    priority: payload.urgency,
    category,
    description: payload.description,
    systemContext: {
      application: payload.systemName,
      environment: 'demo',
      region: 'corp',
      lastDeployment: '演示模式',
      affectedUsers: payload.urgency === 'P1' ? '多团队可能受影响' : '局部团队受影响'
    },
    errorLogs: payload.errorLog ? payload.errorLog.split('\n') : ['暂无错误日志。'],
    businessContext: ['当前为前端 demo 模式，数据保存在浏览器内存中。', '建议草案仍需人工确认后才能执行。'],
    timeline: [
      { time: '刚刚', state: 'PENDING_CLASSIFICATION', actor: '系统', note: '工单提交，进入规则分类。' },
      { time: '刚刚', state: 'PENDING_PROCESS', actor: '规则引擎', note: '完成规则分类和建议草案生成。' }
    ],
    knowledgeDraft: null
  }
  const analysis: AiAnalysis = {
    ticketId: id,
    classification: category,
    classificationReason: `本地 demo 规则根据标题、描述和日志识别为「${category}」，置信度 ${confidence}%。`,
    confidence,
    confirmationState: '待人工确认',
    knowledgeHits: [],
    troubleshootingSteps: ['复核工单描述、系统名称、错误日志与影响范围。', '确认是否需要升级处理或补充信息。', '处理动作必须由人工确认。'],
    replySuggestion: `已根据本地规则将该工单识别为「${category}」，支持人员会先人工确认再执行后续处理。`,
    riskNotes: ['demo 模式不调用真实后端。', '涉及生产配置、权限或数据修复的动作不得自动执行。']
  }
  tickets = [summary, ...tickets]
  ticketDetails[id] = detail
  analyses[id] = analysis
  return wait(detail)
}

export function demoUpdateTicketStatus(id: string, status: TicketStatus, note: string, resolvedSummary = '') {
  const detail = ensureTicket(id)
  detail.status = status
  detail.timeline.push({
    time: '刚刚',
    state: status,
    actor: '支持人员',
    note: note || `人工确认状态流转为${statusLabel[status]}。`
  })
  if (resolvedSummary) {
    detail.businessContext = [...detail.businessContext, resolvedSummary]
  }
  syncSummary(detail)
  return wait(detail)
}

export function demoCreateKnowledgeDraft(id: string) {
  const detail = ensureTicket(id)
  const draft: KnowledgeDraft = detail.knowledgeDraft ?? {
    articleNo: `KB-DRAFT-${id}`,
    title: `【草稿】${detail.title}`,
    category: detail.category,
    status: 'DRAFT',
    owner: '知识审核人'
  }
  detail.knowledgeDraft = draft
  detail.timeline.push({
    time: '刚刚',
    state: detail.status,
    actor: '规则模板',
    note: '已生成知识草稿，等待人工审核。'
  })
  return wait(draft)
}

export function demoConfirmKnowledgeDraft(articleNo: string) {
  const detail = Object.values(ticketDetails).find((ticket) => ticket.knowledgeDraft?.articleNo === articleNo)
  if (!detail?.knowledgeDraft) {
    throw new Error(`Demo knowledge draft not found: ${articleNo}`)
  }
  detail.knowledgeDraft.status = 'PUBLISHED'
  detail.status = 'KNOWLEDGE_BASED'
  detail.timeline.push({
    time: '刚刚',
    state: 'KNOWLEDGE_BASED',
    actor: '知识审核人',
    note: '人工确认知识草稿并完成知识沉淀。'
  })
  syncSummary(detail)
  return wait(detail.knowledgeDraft)
}
