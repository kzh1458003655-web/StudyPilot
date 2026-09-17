import type {
  MockBackendState,
  MockExamItemRecord,
  MockExamRecord,
  MockProjectRecord,
} from "./types";

const CREATED = {
  empty: "2026-09-17T08:00:00.000Z",
  importing: "2026-09-12T03:20:00.000Z",
  qa: "2026-08-28T12:10:00.000Z",
  frequency: "2026-07-03T01:35:00.000Z",
  exam: "2026-05-16T09:00:00.000Z",
  assessment: "2026-03-02T14:25:00.000Z",
  boundary: "2026-01-01T00:00:00.000Z",
  archived: "2025-12-20T05:45:00.000Z",
} as const;

function operatingSystemItems(baseId: number): MockExamItemRecord[] {
  return [
    {
      id: baseId,
      ordinal: 1,
      type: "SINGLE_CHOICE",
      prompt:
        "在采用时间片轮转调度时，时间片设置过大会使算法趋近于哪一种调度策略？",
      options: ["先来先服务", "最短作业优先", "优先级调度", "多级反馈队列"],
      answer: "A",
      analysis:
        "时间片足够大时，每个进程通常能在一个时间片内运行结束，行为接近 FCFS。",
      knowledgePoint: "进程调度",
      score: 10,
    },
    {
      id: baseId + 1,
      ordinal: 2,
      type: "SINGLE_CHOICE",
      prompt: "下列哪一项不是形成死锁的必要条件？",
      options: ["互斥", "请求并保持", "可剥夺", "循环等待"],
      answer: "C",
      analysis: "死锁需要不可剥夺条件；可剥夺反而能够用于破坏死锁。",
      knowledgePoint: "死锁",
      score: 10,
    },
    {
      id: baseId + 2,
      ordinal: 3,
      type: "SHORT_ANSWER",
      prompt: "说明虚拟内存中缺页中断的处理过程，并指出页面置换发生的条件。",
      options: [],
      answer:
        "检查合法性，定位外存页面，选择空闲页框或置换页，装入页面，更新页表并重新执行指令。无空闲页框时发生页面置换。",
      analysis:
        "答案应覆盖合法性检查、页框分配、磁盘调入、页表更新和指令重启。",
      knowledgePoint: "虚拟内存",
      score: 40,
    },
    {
      id: baseId + 3,
      ordinal: 4,
      type: "SHORT_ANSWER",
      prompt: "比较进程与线程在资源拥有、调度开销和通信方式上的主要区别。",
      options: [],
      answer:
        "进程是资源分配单位，线程是调度单位；同进程线程共享地址空间，切换和通信成本通常更低。",
      analysis: "从资源边界、调度单位、上下文切换和通信四个角度作答。",
      knowledgePoint: "进程与线程",
      score: 40,
    },
  ];
}

function databaseItems(baseId: number): MockExamItemRecord[] {
  return [
    {
      id: baseId,
      ordinal: 1,
      type: "SINGLE_CHOICE",
      prompt: "关系模式达到第三范式主要消除了哪一种依赖造成的异常？",
      options: ["部分函数依赖", "传递函数依赖", "多值依赖", "连接依赖"],
      answer: "B",
      analysis: "3NF 要求非主属性不传递依赖于候选键。",
      knowledgePoint: "关系规范化",
      score: 10,
    },
    {
      id: baseId + 1,
      ordinal: 2,
      type: "SINGLE_CHOICE",
      prompt: "在可串行化隔离级别下，下列哪类异常应被禁止？",
      options: ["脏读", "不可重复读", "幻读", "以上全部"],
      answer: "D",
      analysis: "可串行化要求并发执行结果等价于某个串行顺序。",
      knowledgePoint: "事务隔离",
      score: 10,
    },
    {
      id: baseId + 2,
      ordinal: 3,
      type: "SHORT_ANSWER",
      prompt: "解释 B+ 树索引为何适合数据库范围查询。",
      options: [],
      answer:
        "B+ 树高度低，内部节点只存键，叶子节点有序并通过链表相连，因此定位起点后可顺序扫描范围。",
      analysis: "需要提到扇出、高度、叶子有序和叶子链表。",
      knowledgePoint: "索引结构",
      score: 40,
    },
    {
      id: baseId + 3,
      ordinal: 4,
      type: "SHORT_ANSWER",
      prompt: "给出一个需要事务保证原子性的业务例子，并说明回滚的作用。",
      options: [],
      answer:
        "转账需要扣款和入账同时成功；任一步骤失败时回滚可撤销已执行修改，避免金额凭空增加或减少。",
      analysis: "示例应包含多个相关写操作以及失败后的数据一致性。",
      knowledgePoint: "事务 ACID",
      score: 40,
    },
  ];
}

function exam(
  id: number,
  title: string,
  items: MockExamItemRecord[],
): MockExamRecord {
  return { id, title, items };
}

const projects: MockProjectRecord[] = [
  {
    id: 101,
    name: "01 · 空白项目｜高等数学",
    description: "刚创建、尚未上传资料，用于检查所有模块的初始空状态。",
    createdAt: CREATED.empty,
    archivedAt: null,
    documents: [],
    qaSession: null,
    knowledgePoints: [],
    analyses: [],
    exams: [],
    attempts: [],
  },
  {
    id: 102,
    name: "02 · 资料处理中｜计算机网络",
    description:
      "包含等待、处理中和失败资料，用于检查状态标签、禁用操作及异常文件名。",
    createdAt: CREATED.importing,
    archivedAt: null,
    documents: [
      {
        id: 201,
        indexId: "10000000-0000-4000-8000-000000000201",
        displayName: "计算机网络课程讲义（含 TCP 拥塞控制与可靠传输）.pdf",
        documentType: "LECTURE",
        status: "PROCESSING",
        pageCount: 186,
        chunkCount: 0,
      },
      {
        id: 202,
        indexId: "10000000-0000-4000-8000-000000000202",
        displayName: "2025 年计算机网络期末试卷-扫描件-无法提取文字.pdf",
        documentType: "PAST_EXAM",
        status: "FAILED",
        pageCount: 0,
        chunkCount: 0,
      },
      {
        id: 203,
        indexId: "10000000-0000-4000-8000-000000000203",
        displayName: "复习知识点清单.pdf",
        documentType: "KNOWLEDGE",
        status: "PENDING",
        pageCount: 12,
        chunkCount: 0,
      },
    ],
    qaSession: null,
    knowledgePoints: [],
    analyses: [],
    exams: [],
    attempts: [],
    behavior: { latencyMs: 900 },
  },
  {
    id: 103,
    name: "03 · 问答进行中｜数据结构",
    description: "资料已建立索引，包含多轮问答、多个引用和一次证据不足回答。",
    createdAt: CREATED.qa,
    archivedAt: null,
    documents: [
      {
        id: 301,
        indexId: "10000000-0000-4000-8000-000000000301",
        displayName: "数据结构课程讲义.pdf",
        documentType: "LECTURE",
        status: "READY",
        pageCount: 242,
        chunkCount: 691,
      },
      {
        id: 302,
        indexId: "10000000-0000-4000-8000-000000000302",
        displayName: "算法复杂度与图算法复习提纲.pdf",
        documentType: "KNOWLEDGE",
        status: "READY",
        pageCount: 38,
        chunkCount: 104,
      },
    ],
    qaSession: {
      id: 401,
      turns: [
        {
          question: "为什么二叉搜索树在最坏情况下会退化成链表？",
          status: "ANSWERED",
          answer:
            "当输入序列已经有序且没有平衡机制时，新节点会连续落在同一侧，树高增长到 n，查找复杂度由平均 O(log n) 退化为 O(n)。",
          citations: [
            {
              documentName: "数据结构课程讲义.pdf",
              pageNumber: 67,
              excerpt:
                "若关键字按单调次序插入，二叉排序树会形成只有左子树或右子树的单支树，其平均查找长度与顺序查找相近。",
              score: 0.9621,
            },
          ],
        },
        {
          question: "请比较 Prim 和 Kruskal 算法，最好说明分别适合什么图。",
          status: "ANSWERED",
          answer:
            "Prim 从一个顶点逐步扩展连通树，配合邻接矩阵时更适合稠密图；Kruskal 按边权排序并用并查集避免成环，更适合边数较少的稀疏图。两者都要求无向连通带权图，并产生最小生成树。",
          citations: [
            {
              documentName: "算法复杂度与图算法复习提纲.pdf",
              pageNumber: 21,
              excerpt:
                "Prim 算法的选择对象是跨越已选顶点集合的最小权边，基本实现复杂度为 O(V²)。",
              score: 0.9418,
            },
            {
              documentName: "数据结构课程讲义.pdf",
              pageNumber: 189,
              excerpt:
                "Kruskal 算法依次考察按权值排序的边，若两个端点属于不同连通分量则选取该边。",
              score: 0.9134,
            },
          ],
        },
        {
          question: "讲义里有没有讲红黑树删除修复的所有分类？",
          status: "INSUFFICIENT_EVIDENCE",
          answer:
            "当前资料只介绍了红黑树的基本性质和插入调整，没有找到足够内容支持完整列出删除修复分类。建议补充教材对应章节后再提问。",
          citations: [],
        },
      ],
    },
    knowledgePoints: [],
    analyses: [],
    exams: [],
    attempts: [],
  },
  {
    id: 104,
    name: "04 · 考频未分析｜数据库系统原理",
    description:
      "包含三套尚未分析的数据库系统原理往年试卷，用于演示完整考频分析流程。",
    createdAt: CREATED.frequency,
    archivedAt: null,
    documents: [
      {
        id: 401,
        indexId: "10000000-0000-4000-8000-000000000401",
        displayName: "2012—2013 学年中文卷（含答案）.pdf",
        documentType: "PAST_EXAM",
        status: "READY",
        pageCount: 6,
        chunkCount: 21,
      },
      {
        id: 402,
        indexId: "10000000-0000-4000-8000-000000000402",
        displayName: "2012—2013 学年双语期中 A 卷（含答案）.pdf",
        documentType: "PAST_EXAM",
        status: "READY",
        pageCount: 10,
        chunkCount: 31,
      },
      {
        id: 403,
        indexId: "10000000-0000-4000-8000-000000000403",
        displayName: "2014—2015 学年试题（OCR 版）.pdf",
        documentType: "PAST_EXAM",
        status: "READY",
        pageCount: 16,
        chunkCount: 15,
      },
    ],
    qaSession: null,
    knowledgePoints: [],
    analyses: [],
    exams: [],
    attempts: [],
  },
  {
    id: 105,
    name: "05 · 已生成试卷｜数据库原理",
    description: "已有多套模拟卷，覆盖普通标题、超长标题以及完整四题结构。",
    createdAt: CREATED.exam,
    archivedAt: null,
    documents: [
      {
        id: 501,
        indexId: "10000000-0000-4000-8000-000000000501",
        displayName: "数据库系统概论课程资料.pdf",
        documentType: "TEXTBOOK",
        status: "READY",
        pageCount: 689,
        chunkCount: 2048,
      },
      {
        id: 502,
        indexId: "10000000-0000-4000-8000-000000000502",
        displayName: "数据库历年真题与参考答案合集.pdf",
        documentType: "PAST_EXAM",
        status: "READY",
        pageCount: 96,
        chunkCount: 327,
      },
    ],
    qaSession: null,
    knowledgePoints: [
      { knowledgePoint: "SQL 查询", questionCount: 21 },
      { knowledgePoint: "关系规范化", questionCount: 16 },
      { knowledgePoint: "事务隔离", questionCount: 12 },
      { knowledgePoint: "索引结构", questionCount: 9 },
    ],
    analyses: [
      {
        documentId: 502,
        detectedQuestions: 64,
        analyzedQuestions: 60,
        needsReviewQuestions: 4,
      },
    ],
    exams: [
      exam(601, "数据库原理 · 期末综合练习", databaseItems(7001)),
      exam(
        602,
        "数据库原理 · 事务并发控制、关系规范化与索引结构专项强化练习（中等偏难）",
        databaseItems(7011),
      ),
    ],
    attempts: [],
  },
  {
    id: 106,
    name: "06 · 已完成测评｜操作系统冲刺",
    description:
      "包含已评分作答、满分边界、零分题目与较长反馈，用于测评结果展示。",
    createdAt: CREATED.assessment,
    archivedAt: null,
    documents: [
      {
        id: 601,
        indexId: "10000000-0000-4000-8000-000000000601",
        displayName: "操作系统冲刺讲义.pdf",
        documentType: "KNOWLEDGE",
        status: "READY",
        pageCount: 72,
        chunkCount: 238,
      },
    ],
    qaSession: null,
    knowledgePoints: [
      { knowledgePoint: "进程调度", questionCount: 11 },
      { knowledgePoint: "虚拟内存", questionCount: 10 },
      { knowledgePoint: "死锁", questionCount: 8 },
    ],
    analyses: [],
    exams: [exam(701, "操作系统考前冲刺模拟卷", operatingSystemItems(8001))],
    attempts: [
      {
        id: 901,
        examId: 701,
        status: "GRADED",
        totalScore: 72,
        answers: [
          {
            itemId: 8001,
            answer: "A",
            score: 10,
            feedback: "选择正确。",
            gradingStatus: "RULE_GRADED",
          },
          {
            itemId: 8002,
            answer: "B",
            score: 0,
            feedback: "不可剥夺才是死锁必要条件；可剥夺能够破坏死锁。",
            gradingStatus: "RULE_GRADED",
          },
          {
            itemId: 8003,
            answer: "说明了调页与页表更新，但遗漏了合法性检查和指令重新执行。",
            score: 30,
            feedback: "主流程正确，建议补充异常检查和恢复执行步骤。",
            gradingStatus: "MODEL_GRADED",
          },
          {
            itemId: 8004,
            answer: "进程拥有资源，线程负责执行；线程切换较快。",
            score: 32,
            feedback:
              "覆盖资源和开销，尚需说明同进程线程共享地址空间及通信差异。",
            gradingStatus: "MODEL_GRADED",
          },
        ],
      },
    ],
  },
  {
    id: 107,
    name: "07 · 边界与异常｜名称特别长的课程用于检查侧栏省略、换行和窄屏布局",
    description:
      "该项目保留一段超长问答，并故意让试卷和测评接口返回错误，用于文本溢出与错误状态检查。",
    createdAt: CREATED.boundary,
    archivedAt: null,
    documents: [],
    qaSession: {
      id: 407,
      turns: [
        {
          question:
            "这是一条用于测试极长问题文本在窄屏、宽屏以及浏览器缩放场景下是否能够正常换行且不会挤压引用卡片和输入区域的问题，请给出同样较长的回答。",
          status: "ANSWERED",
          answer:
            "这是一段专门用于视觉边界测试的长回答。它包含连续中文、EnglishWordsWithoutUnexpectedLayoutBreaks、数字 1234567890、标点符号，以及多个句子。页面应保持可读行宽，长内容需要自然换行，不能突破聊天面板，也不能让右侧资料卡片产生水平滚动。实际样式调整时还应检查 125% 与 150% 缩放、390 像素移动端宽度，以及引用详情展开后的高度变化。",
          citations: [
            {
              documentName:
                "一份名称非常非常长、用于验证引用标题截断和详情布局的课程资料文档.pdf",
              pageNumber: 999,
              excerpt:
                "这段引用快照用于模拟数据库在原资料删除后仍保存的历史引用。它有意包含较多文字，以便检查 details 展开时的内边距、行高、边框和长文本换行。",
              score: 0.500001,
            },
          ],
        },
      ],
    },
    knowledgePoints: [],
    analyses: [],
    exams: [],
    attempts: [],
    behavior: {
      latencyMs: 650,
      failingModules: ["exams", "assessment"],
    },
  },
  {
    id: 108,
    name: "08 · 已归档｜计算机组成原理",
    description:
      "完整学习记录仍按项目保留，但项目当前归档，用于检查恢复操作和归档分组。",
    createdAt: CREATED.archived,
    archivedAt: "2026-09-01T06:30:00.000Z",
    documents: [
      {
        id: 801,
        indexId: "10000000-0000-4000-8000-000000000801",
        displayName: "计算机组成原理完整讲义.pdf",
        documentType: "TEXTBOOK",
        status: "READY",
        pageCount: 432,
        chunkCount: 1216,
      },
    ],
    qaSession: {
      id: 408,
      turns: [
        {
          question: "流水线的数据相关有哪些类型？",
          status: "ANSWERED",
          answer:
            "常见数据相关包括写后读 RAW、读后写 WAR 和写后写 WAW；在顺序五级流水线中最常见的是 RAW。",
          citations: [
            {
              documentName: "计算机组成原理完整讲义.pdf",
              pageNumber: 218,
              excerpt: "指令间的数据相关可分为 RAW、WAR 和 WAW 三类。",
              score: 0.97,
            },
          ],
        },
      ],
    },
    knowledgePoints: [{ knowledgePoint: "指令流水线", questionCount: 14 }],
    analyses: [],
    exams: [exam(801, "计算机组成原理结课练习", operatingSystemItems(9001))],
    attempts: [],
  },
];

export function createMockBackendSeed(): MockBackendState {
  return {
    nextProjectId: 1001,
    nextDocumentId: 2001,
    nextSessionId: 3001,
    nextExamId: 4001,
    nextExamItemId: 5001,
    nextAttemptId: 6001,
    projects: structuredClone(projects),
  };
}

export function createGeneratedExam(
  id: number,
  firstItemId: number,
  projectName: string,
  instructions: string,
): MockExamRecord {
  const title = instructions
    ? `${projectName} · ${instructions.slice(0, 36)}`
    : `${projectName} · 默认综合练习`;
  return exam(id, title, operatingSystemItems(firstItemId));
}
