package com.nexushub.puzzle.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import java.util.List;

public final class PuzzleDtos {
  private PuzzleDtos() {}

  public record SearchRequest(
      @Min(1) Integer pageNum,
      @Min(1) @Max(100) Integer pageSize,
      String scope,
      String keyword,
      String tag,
      String difficulty,
      String status) {}

  public record DetailRequest(@NotBlank String id, String versionId) {}

  public record ProofStrategyRequest(@NotBlank String versionId) {}

  public record ValidateRequest(@NotNull JsonNode state) {}

  public record SaveRequest(
      String id,
      Integer expectedRowVersion,
      @NotBlank @Size(max = 120) String title,
      @Size(max = 2000) String description,
      List<@NotBlank @Size(max = 32) String> tags,
      String difficulty,
      @NotEmpty @Size(max = 2) List<@Min(0) @Max(1) Integer> allowedFirstSeats,
      @NotNull JsonNode state) {}

  public record DuplicateCheckRequest(@NotNull JsonNode state, String excludePuzzleId) {}

  public record DuplicatePuzzle(
      String id,
      String versionId,
      String title,
      String status,
      boolean deleted,
      String stateHash) {}

  public record DuplicateCheck(boolean duplicate, DuplicatePuzzle existing) {}

  public record ArchiveRequest(@NotBlank String id, @NotNull Integer expectedRowVersion) {}

  public record PublishRequest(
      @NotBlank String id, @NotNull Integer expectedRowVersion, @NotBlank String versionId) {}

  public record FieldError(String field, String code, String message) {}

  public record ValidationResult(
      boolean valid, List<FieldError> errors, JsonNode canonicalState, String stateHash) {}

  public record Version(
      String id,
      int versionNo,
      String title,
      String description,
      List<String> tags,
      String difficulty,
      List<Integer> allowedFirstSeats,
      JsonNode initialState,
      String stateHash) {}

  public record Puzzle(
      String id,
      String ownerId,
      String status,
      int rowVersion,
      String latestVersionId,
      String publishedVersionId,
      Version version,
      List<String> allowedActions,
      String proofStatus,
      String winnerSide) {}

  public record Page(List<Puzzle> items, long total, int pageNum, int pageSize) {}
}
