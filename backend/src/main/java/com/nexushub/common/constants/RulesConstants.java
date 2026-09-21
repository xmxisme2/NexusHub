package com.nexushub.common.constants;

/** 规则契约公共常量；规则版本变更时必须同步 API、数据库快照和前端类型。 */
public final class RulesConstants {
  /** 当前双人明牌斗地主规则版本。 */
  public static final String CLASSIC_V1 = "CLASSIC_V1";

  /** 当前规则状态快照的结构版本。 */
  public static final int STATE_SCHEMA_VERSION = 1;

  private RulesConstants() {}
}
