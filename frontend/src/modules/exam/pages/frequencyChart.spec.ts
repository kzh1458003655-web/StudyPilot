import { describe, expect, it } from "vitest";
import { createFrequencyChartOption, sortFrequencies } from "./frequencyChart";

describe("frequency chart", () => {
  const points = [
    { knowledgePoint: "低频", questionCount: 2 },
    { knowledgePoint: "高频", questionCount: 8 },
    { knowledgePoint: "中频", questionCount: 5 },
  ];

  it("sorts points descending and highlights only the highest frequency", () => {
    expect(sortFrequencies(points).map((item) => item.knowledgePoint)).toEqual([
      "高频",
      "中频",
      "低频",
    ]);

    const option = createFrequencyChartOption(points);
    expect(option.yAxis.data).toEqual(["高频", "中频", "低频"]);
    expect(option.series[0].data[0].itemStyle.color).toBe("#c96c4d");
    expect(option.series[0].data[1].itemStyle.color).not.toBe("#c96c4d");
  });

  it("shows count and percentage in the tooltip", () => {
    const option = createFrequencyChartOption(points);
    expect(option.tooltip.formatter([{ name: "高频", value: 8 }])).toContain(
      "53%",
    );
  });
});
