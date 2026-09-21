package com.nexushub.common.constants;

/** 残局生命周期公共常量；数据库状态与 API 枚举必须保持一致。 */
public final class PuzzleConstants {
  /** 草稿状态。 */
  public static final String STATUS_DRAFT = "DRAFT";

  /** 已发布状态。 */
  public static final String STATUS_PUBLISHED = "PUBLISHED";

  /** 逻辑删除状态。 */
  public static final String STATUS_ARCHIVED = "ARCHIVED";

  private PuzzleConstants() {}
}
