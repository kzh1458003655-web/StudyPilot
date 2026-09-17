import type { KnowledgePointFrequency } from "../types/domain";

const PRIMARY = "#c96c4d";
const INK = "#3b3732";
const MUTED = "#766f67";
const GRID = "#eee8e1";
const PAPER = "#fffdf9";
const SECONDARY = "#557266";
const mutedBars = ["#9f9388", "#b9aea4", "#d1c7be", "#e1d9d1"];

export interface ExamTopicInsight {
  name: string;
  shortName: string;
  paperCount: number;
  score: number;
  share: number;
  priority: number;
}

export const EXAM_TOPIC_INSIGHTS: ExamTopicInsight[] = [
  {
    name: "函数依赖与关系规范化",
    shortName: "关系规范化",
    paperCount: 3,
    score: 108,
    share: 36,
    priority: 96,
  },
  {
    name: "关系代数与查询表达",
    shortName: "关系代数",
    paperCount: 3,
    score: 65,
    share: 21.7,
    priority: 88,
  },
  {
    name: "E-R 建模与数据库设计",
    shortName: "E-R 建模",
    paperCount: 3,
    score: 41,
    share: 13.7,
    priority: 80,
  },
  {
    name: "并发控制与事务",
    shortName: "并发控制",
    paperCount: 1,
    score: 26,
    share: 8.7,
    priority: 72,
  },
  {
    name: "数据库基础与体系结构",
    shortName: "数据库基础",
    paperCount: 2,
    score: 22,
    share: 7.3,
    priority: 58,
  },
  {
    name: "SQL 与完整性约束",
    shortName: "SQL 与约束",
    paperCount: 1,
    score: 18,
    share: 6,
    priority: 55,
  },
  {
    name: "选答理论",
    shortName: "选答理论",
    paperCount: 1,
    score: 18,
    share: 6,
    priority: 42,
  },
  {
    name: "DBA 职责",
    shortName: "DBA 职责",
    paperCount: 1,
    score: 2,
    share: 0.7,
    priority: 25,
  },
];

export function sortFrequencies(items: KnowledgePointFrequency[]) {
  return [...items].sort((a, b) => b.questionCount - a.questionCount);
}

export function createFrequencyChartOption(items: KnowledgePointFrequency[]) {
  const sorted = sortFrequencies(items);
  const total = sorted.reduce((sum, item) => sum + item.questionCount, 0);

  return {
    animationDuration: 350,
    animationEasing: "cubicOut",
    grid: { left: 150, right: 52, top: 8, bottom: 8 },
    tooltip: {
      trigger: "axis",
      axisPointer: { type: "shadow" },
      backgroundColor: PAPER,
      borderColor: "#e5ded5",
      textStyle: { color: "#25231f" },
      formatter: (params: Array<{ name: string; value: number }>) => {
        const item = params[0];
        const percent = total ? Math.round((item.value / total) * 100) : 0;
        return `<strong>${item.name}</strong><br/>${item.value} 题 · ${percent}%`;
      },
    },
    xAxis: {
      type: "value",
      minInterval: 1,
      axisLabel: { color: MUTED },
      splitLine: { lineStyle: { color: GRID } },
    },
    yAxis: {
      type: "category",
      inverse: true,
      data: sorted.map((item) => item.knowledgePoint),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: INK, width: 130, overflow: "truncate" },
    },
    series: [
      {
        type: "bar",
        data: sorted.map((item, index) => ({
          value: item.questionCount,
          itemStyle: {
            color:
              index === 0
                ? PRIMARY
                : mutedBars[Math.min(index - 1, mutedBars.length - 1)],
          },
        })),
        barMaxWidth: 28,
        label: {
          show: true,
          position: "right",
          color: MUTED,
          formatter: "{c} 题",
        },
        itemStyle: { borderRadius: [0, 6, 6, 0] },
      },
    ],
  };
}

export function createScoreFrequencyChartOption() {
  return {
    animationDuration: 380,
    animationEasing: "cubicOut",
    grid: { left: 118, right: 54, top: 8, bottom: 28 },
    tooltip: {
      trigger: "axis",
      axisPointer: { type: "shadow" },
      backgroundColor: PAPER,
      borderColor: "#e5ded5",
      textStyle: { color: "#25231f" },
      formatter: (params: Array<{ data: ExamTopicInsight }>) => {
        const item = params[0].data;
        return `<strong>${item.name}</strong><br/>${item.score} 分 · ${item.share}% · 覆盖 ${item.paperCount}/3 套`;
      },
    },
    xAxis: {
      type: "value",
      max: 40,
      axisLabel: { color: MUTED, formatter: "{value}%" },
      splitLine: { lineStyle: { color: GRID } },
    },
    yAxis: {
      type: "category",
      inverse: true,
      data: EXAM_TOPIC_INSIGHTS.map((item) => item.shortName),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: INK, width: 104, overflow: "truncate" },
    },
    series: [
      {
        type: "bar",
        data: EXAM_TOPIC_INSIGHTS.map((item, index) => ({
          ...item,
          value: item.share,
          itemStyle: {
            color:
              index < 3 ? [PRIMARY, "#d99578", "#557266"][index] : "#cfc5bb",
          },
        })),
        barMaxWidth: 22,
        label: {
          show: true,
          position: "right",
          color: MUTED,
          formatter: ({ value }: { value: number }) => `${value}%`,
        },
        itemStyle: { borderRadius: [0, 5, 5, 0] },
      },
    ],
  };
}

export function createPriorityRadarOption() {
  const radarItems = EXAM_TOPIC_INSIGHTS.slice(0, 6);
  return {
    animationDuration: 320,
    tooltip: {
      backgroundColor: PAPER,
      borderColor: "#e5ded5",
      textStyle: { color: "#25231f" },
    },
    radar: {
      radius: "64%",
      splitNumber: 4,
      indicator: radarItems.map((item) => ({
        name: item.shortName,
        max: 100,
      })),
      axisName: { color: INK, fontSize: 11 },
      axisLine: { lineStyle: { color: "#d9cfc5" } },
      splitLine: { lineStyle: { color: "#e8e0d8" } },
      splitArea: {
        areaStyle: { color: ["#fffdf9", "#f8f3ed"] },
      },
    },
    series: [
      {
        type: "radar",
        data: [
          {
            name: "复习优先级",
            value: radarItems.map((item) => item.priority),
            areaStyle: { color: "rgba(201, 108, 77, 0.20)" },
            lineStyle: { color: PRIMARY, width: 2 },
            itemStyle: { color: PRIMARY },
            symbolSize: 5,
          },
        ],
      },
    ],
  };
}

export function createFrequencyQuadrantOption() {
  return {
    animationDuration: 320,
    grid: { left: 48, right: 22, top: 18, bottom: 44 },
    tooltip: {
      backgroundColor: PAPER,
      borderColor: "#e5ded5",
      textStyle: { color: "#25231f" },
      formatter: (params: { data: [number, number, number, string] }) => {
        const [coverage, averageScore, score, name] = params.data;
        return `<strong>${name}</strong><br/>覆盖率 ${coverage}% · 出现时平均 ${averageScore} 分<br/>三套合计 ${score} 分`;
      },
    },
    xAxis: {
      name: "试卷覆盖率",
      nameLocation: "middle",
      nameGap: 30,
      min: 20,
      max: 105,
      axisLabel: { color: MUTED, formatter: "{value}%" },
      splitLine: { lineStyle: { color: GRID } },
    },
    yAxis: {
      name: "出现时平均分值",
      min: 0,
      max: 40,
      axisLabel: { color: MUTED },
      splitLine: { lineStyle: { color: GRID } },
    },
    series: [
      {
        type: "scatter",
        symbolSize: (data: [number, number, number]) =>
          Math.max(12, Math.sqrt(data[2]) * 3.6),
        data: EXAM_TOPIC_INSIGHTS.slice(0, 6).map((item) => [
          Math.round((item.paperCount / 3) * 100),
          Number((item.score / item.paperCount).toFixed(1)),
          item.score,
          item.shortName,
        ]),
        itemStyle: { color: PRIMARY, opacity: 0.84 },
        label: {
          show: true,
          position: "top",
          color: INK,
          fontSize: 10,
          formatter: ({ data }: { data: [number, number, number, string] }) =>
            data[3],
        },
        markLine: {
          silent: true,
          symbol: "none",
          lineStyle: { color: "#b9aea4", type: "dashed" },
          label: { show: false },
          data: [{ xAxis: 67 }, { yAxis: 16 }],
        },
      },
    ],
  };
}

export function createFrequencyRelationOption() {
  const nodes = [
    { name: "函数依赖", x: 210, y: 142, symbolSize: 58, category: 0 },
    { name: "候选码", x: 78, y: 54, symbolSize: 38, category: 1 },
    { name: "属性闭包", x: 72, y: 148, symbolSize: 38, category: 1 },
    { name: "范式判断", x: 84, y: 240, symbolSize: 40, category: 1 },
    { name: "3NF 分解", x: 238, y: 270, symbolSize: 44, category: 1 },
    { name: "无损连接", x: 360, y: 220, symbolSize: 40, category: 1 },
    { name: "依赖保持", x: 370, y: 72, symbolSize: 40, category: 1 },
    { name: "关系代数", x: 514, y: 94, symbolSize: 52, category: 2 },
    { name: "查询表达", x: 646, y: 52, symbolSize: 38, category: 3 },
    { name: "关系除法", x: 662, y: 142, symbolSize: 38, category: 3 },
    { name: "E-R 建模", x: 520, y: 236, symbolSize: 48, category: 2 },
    { name: "关系转换", x: 660, y: 252, symbolSize: 38, category: 3 },
  ];
  const edges = [
    ["函数依赖", "候选码"],
    ["函数依赖", "属性闭包"],
    ["函数依赖", "范式判断"],
    ["范式判断", "3NF 分解"],
    ["3NF 分解", "无损连接"],
    ["3NF 分解", "依赖保持"],
    ["依赖保持", "关系代数"],
    ["关系代数", "查询表达"],
    ["关系代数", "关系除法"],
    ["无损连接", "E-R 建模"],
    ["E-R 建模", "关系转换"],
  ].map(([source, target]) => ({ source, target }));

  return {
    animationDuration: 0,
    tooltip: {
      backgroundColor: PAPER,
      borderColor: "#e5ded5",
      textStyle: { color: "#25231f" },
    },
    series: [
      {
        type: "graph",
        layout: "none",
        roam: false,
        data: nodes,
        links: edges,
        categories: [
          { name: "核心", itemStyle: { color: PRIMARY } },
          { name: "关系理论", itemStyle: { color: "#dca087" } },
          { name: "模型", itemStyle: { color: SECONDARY } },
          { name: "应用", itemStyle: { color: "#9aaa9f" } },
        ],
        label: { show: true, color: "#fffdf9", fontSize: 10 },
        edgeSymbol: ["none", "arrow"],
        edgeSymbolSize: 6,
        lineStyle: { color: "#bfb4a9", width: 1.2, curveness: 0.08 },
        emphasis: { focus: "adjacency" },
      },
    ],
  };
}
