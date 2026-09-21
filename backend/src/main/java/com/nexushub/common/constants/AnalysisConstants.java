package com.nexushub.common.constants;

/** 求解任务公共常量；来源类型和求解器版本参与任务幂等键。 */
public final class AnalysisConstants {
  /** 已完成严格胜负证明。 */
  public static final String PROOF_PROVEN = "PROVEN";

  /** 尚无严格胜负证明，不能展示必胜方。 */
  public static final String PROOF_UNKNOWN = "UNKNOWN";

  /** 任务用途：分析。 */
  public static final String PURPOSE_ANALYSIS = "ANALYSIS";

  /** 求解来源：残局版本。 */
  public static final String SOURCE_PUZZLE_VERSION = "PUZZLE_VERSION";

  /** 求解来源：临时自定义局面。 */
  public static final String SOURCE_CUSTOM = "CUSTOM";

  /** 当前 Minimax 求解器版本。 */
  public static final String SOLVER_VERSION = "MINIMAX_V1";

  private AnalysisConstants() {}
}
