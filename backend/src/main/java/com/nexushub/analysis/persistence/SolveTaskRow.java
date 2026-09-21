package com.nexushub.analysis.persistence;

import java.time.Instant;

/** solve_task 持久行；任务输入为不可变快照，Worker 通过租约和版本状态提交结果。 */
public class SolveTaskRow {
  private Long id, ownerId, puzzleVersionId, gameId, nodeLimit;
  private Integer gameStateVersion, timeLimitMs, memoryLimitMb, attemptCount;
  private String purpose,
      sourceType,
      stateSnapshot,
      stateHash,
      rulesetVersion,
      solverVersion,
      taskStatus,
      terminationReason,
      leaseToken,
      statsJson,
      errorCode;
  private Instant leaseUntil, availableTime, startTime, finishTime, createTime;

  public Long getId() {
    return id;
  }

  public void setId(Long v) {
    id = v;
  }

  public Long getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(Long v) {
    ownerId = v;
  }

  public Long getPuzzleVersionId() {
    return puzzleVersionId;
  }

  public void setPuzzleVersionId(Long v) {
    puzzleVersionId = v;
  }

  public Long getGameId() {
    return gameId;
  }

  public void setGameId(Long v) {
    gameId = v;
  }

  public Long getNodeLimit() {
    return nodeLimit;
  }

  public void setNodeLimit(Long v) {
    nodeLimit = v;
  }

  public Integer getGameStateVersion() {
    return gameStateVersion;
  }

  public void setGameStateVersion(Integer v) {
    gameStateVersion = v;
  }

  public Integer getTimeLimitMs() {
    return timeLimitMs;
  }

  public void setTimeLimitMs(Integer v) {
    timeLimitMs = v;
  }

  public Integer getMemoryLimitMb() {
    return memoryLimitMb;
  }

  public void setMemoryLimitMb(Integer v) {
    memoryLimitMb = v;
  }

  public Integer getAttemptCount() {
    return attemptCount;
  }

  public void setAttemptCount(Integer v) {
    attemptCount = v;
  }

  public String getPurpose() {
    return purpose;
  }

  public void setPurpose(String v) {
    purpose = v;
  }

  public String getSourceType() {
    return sourceType;
  }

  public void setSourceType(String v) {
    sourceType = v;
  }

  public String getStateSnapshot() {
    return stateSnapshot;
  }

  public void setStateSnapshot(String v) {
    stateSnapshot = v;
  }

  public String getStateHash() {
    return stateHash;
  }

  public void setStateHash(String v) {
    stateHash = v;
  }

  public String getRulesetVersion() {
    return rulesetVersion;
  }

  public void setRulesetVersion(String v) {
    rulesetVersion = v;
  }

  public String getSolverVersion() {
    return solverVersion;
  }

  public void setSolverVersion(String v) {
    solverVersion = v;
  }

  public String getTaskStatus() {
    return taskStatus;
  }

  public void setTaskStatus(String v) {
    taskStatus = v;
  }

  public String getTerminationReason() {
    return terminationReason;
  }

  public void setTerminationReason(String v) {
    terminationReason = v;
  }

  public String getLeaseToken() {
    return leaseToken;
  }

  public void setLeaseToken(String v) {
    leaseToken = v;
  }

  public String getStatsJson() {
    return statsJson;
  }

  public void setStatsJson(String v) {
    statsJson = v;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String v) {
    errorCode = v;
  }

  public Instant getLeaseUntil() {
    return leaseUntil;
  }

  public void setLeaseUntil(Instant v) {
    leaseUntil = v;
  }

  public Instant getAvailableTime() {
    return availableTime;
  }

  public void setAvailableTime(Instant v) {
    availableTime = v;
  }

  public Instant getStartTime() {
    return startTime;
  }

  public void setStartTime(Instant v) {
    startTime = v;
  }

  public Instant getFinishTime() {
    return finishTime;
  }

  public void setFinishTime(Instant v) {
    finishTime = v;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Instant v) {
    createTime = v;
  }
}
