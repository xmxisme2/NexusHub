package com.nexushub.game.persistence;

import java.time.Instant;

/** game_action 插入行；每一次人类行动与状态更新必须在同一事务提交。 */
public class GameActionRow {
  private Long id, gameId;
  private Integer sequenceNo, actorSeat, beforeVersion, afterVersion;
  private String actorType, decisionQuality, moveJson, afterStateJson, requestKey;
  private Instant createTime;

  public Long getId() {
    return id;
  }

  public void setId(Long value) {
    id = value;
  }

  public Long getGameId() {
    return gameId;
  }

  public void setGameId(Long value) {
    gameId = value;
  }

  public Integer getSequenceNo() {
    return sequenceNo;
  }

  public void setSequenceNo(Integer value) {
    sequenceNo = value;
  }

  public Integer getActorSeat() {
    return actorSeat;
  }

  public void setActorSeat(Integer value) {
    actorSeat = value;
  }

  public Integer getBeforeVersion() {
    return beforeVersion;
  }

  public void setBeforeVersion(Integer value) {
    beforeVersion = value;
  }

  public Integer getAfterVersion() {
    return afterVersion;
  }

  public void setAfterVersion(Integer value) {
    afterVersion = value;
  }

  public String getActorType() {
    return actorType;
  }

  public void setActorType(String value) {
    actorType = value;
  }

  public String getDecisionQuality() {
    return decisionQuality;
  }

  public void setDecisionQuality(String value) {
    decisionQuality = value;
  }

  public String getMoveJson() {
    return moveJson;
  }

  public void setMoveJson(String value) {
    moveJson = value;
  }

  public String getAfterStateJson() {
    return afterStateJson;
  }

  public void setAfterStateJson(String value) {
    afterStateJson = value;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public void setRequestKey(String value) {
    requestKey = value;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Instant value) {
    createTime = value;
  }
}
