package cn.studypilot.assessment.dto;
import jakarta.validation.constraints.*; import java.util.List;
public record SubmitAttemptRequest(@NotEmpty List<Answer> answers){public record Answer(@Positive long itemId,@NotBlank @Size(max=10000) String answer){}}
