import type { KnowledgePointFrequency } from "../types/domain";

const mutedBars = ["#9f9388", "#b9aea4", "#d1c7be", "#e1d9d1"];

export function sortFrequencies(items: KnowledgePointFrequency[]) {
  return [...items].sort((a, b) => b.questionCount - a.questionCount);
}

export function createFrequencyChartOption(items: KnowledgePointFrequency[]) {
  const sorted = sortFrequencies(items);
  const total = sorted.reduce((sum, item) => sum + item.questionCount, 0);

  return {
    animationDuration: 700,
    animationEasing: "cubicOut",
    grid: { left: 150, right: 52, top: 8, bottom: 8 },
    tooltip: {
      trigger: "axis",
      axisPointer: { type: "shadow" },
      backgroundColor: "#fffdf9",
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
      axisLabel: { color: "#766f67" },
      splitLine: { lineStyle: { color: "#eee8e1" } },
    },
    yAxis: {
      type: "category",
      inverse: true,
      data: sorted.map((item) => item.knowledgePoint),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: "#3b3732", width: 130, overflow: "truncate" },
    },
    series: [
      {
        type: "bar",
        data: sorted.map((item, index) => ({
          value: item.questionCount,
          itemStyle: {
            color:
              index === 0
                ? "#c96c4d"
                : mutedBars[Math.min(index - 1, mutedBars.length - 1)],
          },
        })),
        barMaxWidth: 28,
        label: {
          show: true,
          position: "right",
          color: "#766f67",
          formatter: "{c} 题",
        },
        itemStyle: { borderRadius: [0, 6, 6, 0] },
      },
    ],
  };
}
