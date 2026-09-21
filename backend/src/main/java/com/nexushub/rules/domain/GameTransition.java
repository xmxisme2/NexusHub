package com.nexushub.rules.domain;

/** 服务端状态转移结果；winnerSide 仅在手牌归零时存在。 */
public record GameTransition(GameState state, Seat winnerSide) {}
