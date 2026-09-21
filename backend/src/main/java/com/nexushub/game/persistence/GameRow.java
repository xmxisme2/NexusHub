package com.nexushub.game.persistence;

import java.time.Instant;

/** game_session 查询行，JSON 状态由 Service 转成规则层不可变对象。 */
public class GameRow {
  private Long id, ownerId, puzzleVersionId;
  private Integer firstSeat, stateVersion, hintCount;
  private String mode, status, initialStateJson, currentStateJson, winnerSide, robotStatus;
  private Instant createTime;

  public Long getId() {
    return id;
  }

  public void setId(Long value) {
    id = value;
  }

  public Long getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(Long value) {
    ownerId = value;
  }

  public Long getPuzzleVersionId() {
    return puzzleVersionId;
  }

  public void setPuzzleVersionId(Long value) {
    puzzleVersionId = value;
  }

  public Integer getFirstSeat() {
    return firstSeat;
  }

  public void setFirstSeat(Integer value) {
    firstSeat = value;
  }

  public Integer getStateVersion() {
    return stateVersion;
  }

  public void setStateVersion(Integer value) {
    stateVersion = value;
  }

  public Integer getHintCount() {
    return hintCount;
  }

  public void setHintCount(Integer value) {
    hintCount = value;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String value) {
    mode = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }

  public String getInitialStateJson() {
    return initialStateJson;
  }

  public void setInitialStateJson(String value) {
    initialStateJson = value;
  }

  public String getCurrentStateJson() {
    return currentStateJson;
  }

  public void setCurrentStateJson(String value) {
    currentStateJson = value;
  }

  public String getWinnerSide() {
    return winnerSide;
  }

  public void setWinnerSide(String value) {
    winnerSide = value;
  }

  public String getRobotStatus() {
    return robotStatus;
  }

  public void setRobotStatus(String value) {
    robotStatus = value;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Instant value) {
    createTime = value;
  }
}
