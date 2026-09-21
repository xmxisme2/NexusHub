package com.nexushub.rules.domain;

/** CLASSIC_V1 双人残局手牌不变量校验；不依赖 Spring 或数据库。 */
public final class HandValidator {
  private HandValidator() {}

  public static HandValidationResult validate(int[] user, int[] bot, Seat firstSeat) {
    if (user == null || bot == null || user.length != 15 || bot.length != 15)
      return HandValidationResult.invalid("手牌必须是15个牌点计数");
    if (firstSeat == null) return HandValidationResult.invalid("必须指定先手");
    int userTotal = total(user), botTotal = total(bot);
    if (userTotal == 0 || botTotal == 0) return HandValidationResult.invalid("双方手牌不能为空");
    int firstTotal = firstSeat == Seat.USER ? userTotal : botTotal;
    int secondTotal = firstSeat == Seat.USER ? botTotal : userTotal;
    if (firstTotal > 20 || secondTotal > 17) return HandValidationResult.invalid("先手最多20张，后手最多17张");
    for (int i = 0; i < 15; i++) {
      int max = i >= 13 ? 1 : 4;
      if (user[i] < 0 || bot[i] < 0 || user[i] + bot[i] > max)
        return HandValidationResult.invalid("两位玩家合计的同牌点数量超过牌库上限");
    }
    return HandValidationResult.ok();
  }

  private static int total(int[] cards) {
    int n = 0;
    for (int card : cards) n += card;
    return n;
  }
}
