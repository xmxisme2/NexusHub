package com.nexushub.common.constants;

/** 对局公共常量；前后端状态枚举以此处的契约为准。 */
public final class GameConstants {
  /** 训练模式允许在未证明时使用确定性启发式行动。 */
  public static final String MODE_TRAINING = "TRAINING";

  /** 严格最优模式，每个机器人回合都必须取得 PROVEN 结果。 */
  public static final String MODE_OPTIMAL = "OPTIMAL";

  /** 机器人已排队等待计算。 */
  public static final String ROBOT_QUEUED = "QUEUED";

  /** 机器人正在计算。 */
  public static final String ROBOT_THINKING = "THINKING";

  /** 当前预算无法证明，暂停机器人落子。 */
  public static final String ROBOT_NEEDS_PROOF = "NEEDS_PROOF";

  /** 机器人没有待处理任务。 */
  public static final String ROBOT_IDLE = "IDLE";

  /** 机器人任务发生异常。 */
  public static final String ROBOT_FAILED = "FAILED";

  /** 行动事件的严格最优决策标记。 */
  public static final String DECISION_OPTIMAL = "OPTIMAL";

  /** 行动事件的启发式决策标记。 */
  public static final String DECISION_HEURISTIC = "HEURISTIC";

  /** 对局进行中。 */
  public static final String STATUS_ACTIVE = "ACTIVE";

  /** 对局已结束。 */
  public static final String STATUS_FINISHED = "FINISHED";

  /** 对局已放弃。 */
  public static final String STATUS_ABANDONED = "ABANDONED";

  /** 行动事件由用户产生。 */
  public static final String ACTOR_HUMAN = "HUMAN";

  /** 行动事件由机器人产生。 */
  public static final String ACTOR_BOT = "BOT";

  private GameConstants() {}
}
