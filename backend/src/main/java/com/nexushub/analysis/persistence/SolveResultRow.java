package com.nexushub.analysis.persistence;

import java.time.Instant;

/** solve_result 持久行；PROVEN 只在完整搜索返回时写入。 */
public class SolveResultRow {
  private Long id, taskId, rootNodeId;
  private Integer proofValue;
  private String proofStatus, winnerSide, strategyStatus, winningLinesJson, statsJson;
  private Instant verifiedTime;

  public Long getId() {
    return id;
  }

  public void setId(Long v) {
    id = v;
  }

  public Long getTaskId() {
    return taskId;
  }

  public void setTaskId(Long v) {
    taskId = v;
  }

  public Long getRootNodeId() {
    return rootNodeId;
  }

  public void setRootNodeId(Long v) {
    rootNodeId = v;
  }

  public Integer getProofValue() {
    return proofValue;
  }

  public void setProofValue(Integer v) {
    proofValue = v;
  }

  public String getProofStatus() {
    return proofStatus;
  }

  public void setProofStatus(String v) {
    proofStatus = v;
  }

  public String getWinnerSide() {
    return winnerSide;
  }

  public void setWinnerSide(String v) {
    winnerSide = v;
  }

  public String getStrategyStatus() {
    return strategyStatus;
  }

  public void setStrategyStatus(String v) {
    strategyStatus = v;
  }

  public String getWinningLinesJson() {
    return winningLinesJson;
  }

  public void setWinningLinesJson(String v) {
    winningLinesJson = v;
  }

  public String getStatsJson() {
    return statsJson;
  }

  public void setStatsJson(String v) {
    statsJson = v;
  }

  public Instant getVerifiedTime() {
    return verifiedTime;
  }

  public void setVerifiedTime(Instant v) {
    verifiedTime = v;
  }
}
