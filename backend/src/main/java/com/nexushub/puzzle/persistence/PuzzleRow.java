package com.nexushub.puzzle.persistence;

public class PuzzleRow {
  /** 当前展示版本的严格证明胜方；无匹配证明时为空。 */
  private String winnerSide;

  public String getWinnerSide() {
    return winnerSide;
  }

  public void setWinnerSide(String winnerSide) {
    this.winnerSide = winnerSide;
  }

  private Long id, ownerId, latestVersionId, publishedVersionId, versionId;
  private Integer deleted;
  private String status,
      title,
      description,
      tagsJson,
      difficulty,
      allowedFirstSeatsJson,
      initialStateJson,
      stateHash;
  private Integer rowVersion, versionNo;

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

  public Long getLatestVersionId() {
    return latestVersionId;
  }

  public void setLatestVersionId(Long v) {
    latestVersionId = v;
  }

  public Long getPublishedVersionId() {
    return publishedVersionId;
  }

  public void setPublishedVersionId(Long v) {
    publishedVersionId = v;
  }

  public Long getVersionId() {
    return versionId;
  }

  public void setVersionId(Long v) {
    versionId = v;
  }

  public Integer getDeleted() {
    return deleted;
  }

  public void setDeleted(Integer v) {
    deleted = v;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String v) {
    status = v;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String v) {
    title = v;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String v) {
    description = v;
  }

  public String getTagsJson() {
    return tagsJson;
  }

  public void setTagsJson(String v) {
    tagsJson = v;
  }

  public String getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(String v) {
    difficulty = v;
  }

  public String getAllowedFirstSeatsJson() {
    return allowedFirstSeatsJson;
  }

  public void setAllowedFirstSeatsJson(String v) {
    allowedFirstSeatsJson = v;
  }

  public String getInitialStateJson() {
    return initialStateJson;
  }

  public void setInitialStateJson(String v) {
    initialStateJson = v;
  }

  public String getStateHash() {
    return stateHash;
  }

  public void setStateHash(String v) {
    stateHash = v;
  }

  public Integer getRowVersion() {
    return rowVersion;
  }

  public void setRowVersion(Integer v) {
    rowVersion = v;
  }

  public Integer getVersionNo() {
    return versionNo;
  }

  public void setVersionNo(Integer v) {
    versionNo = v;
  }
}
