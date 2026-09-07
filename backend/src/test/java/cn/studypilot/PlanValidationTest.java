package cn.studypilot;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
class PlanValidationTest {
  ObjectMapper mapper = new ObjectMapper();
  LocalDate start = LocalDate.of(2026, 9, 7);
  JsonNode tasks(String... entries) throws Exception {
    return mapper.readTree("[" + String.join(",", entries) + "]");
  }
  String task(String date, int minutes) {
    return "{\"date\":\"" + date
        + "\",\"title\":\"范式复习\",\"detail\":\"阅读资料并归纳概念\",\"minutes\":" + minutes
        + "}";
  }
  @Test
  void validPlan() throws Exception {
    assertDoesNotThrow(
        ()
            -> PlanService.validate(
                tasks(task("2026-09-07", 30), task("2026-09-07", 30)), start, 3, 60, null));
  }
  @Test
  void overBudget() {
    assertThrows(IllegalArgumentException.class,
        ()
            -> PlanService.validate(
                tasks(task("2026-09-07", 40), task("2026-09-07", 30)), start, 3, 60, null));
  }
  @Test
  void outsideDateRange() {
    assertThrows(IllegalArgumentException.class,
        () -> PlanService.validate(tasks(task("2026-09-10", 30)), start, 3, 60, null));
  }
  @Test
  void malformedDate() {
    assertThrows(IllegalArgumentException.class,
        () -> PlanService.validate(tasks(task("tomorrow", 30)), start, 3, 60, null));
  }
  @Test
  void negativeDuration() {
    assertThrows(IllegalArgumentException.class,
        () -> PlanService.validate(tasks(task("2026-09-07", -1)), start, 3, 60, null));
  }
  @Test
  void emptyPlan() {
    assertThrows(
        IllegalArgumentException.class, () -> PlanService.validate(tasks(), start, 3, 60, null));
  }
  @Test
  void completedTimeCounts() {
    Map<String, Object> existing =
        Map.of("tasks", List.of(Map.of("done", true, "task_date", "2026-09-07", "minutes", 40)));
    assertThrows(IllegalArgumentException.class,
        () -> PlanService.validate(tasks(task("2026-09-07", 30)), start, 3, 60, existing));
  }
  @Test
  void completedPriorDaysPreserved() {
    Map<String, Object> existing =
        Map.of("tasks", List.of(Map.of("done", true, "task_date", "2026-09-06", "minutes", 40)));
    assertDoesNotThrow(
        () -> PlanService.validate(tasks(task("2026-09-07", 30)), start, 3, 60, existing));
  }
}
