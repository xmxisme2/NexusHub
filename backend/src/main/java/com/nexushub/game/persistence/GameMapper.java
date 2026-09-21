package com.nexushub.game.persistence;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GameMapper {
  long nextGameId();

  long nextActionId();

  GameRow findGame(@Param("id") long id);

  List<GameActionRow> listActions(
      @Param("gameId") long gameId, @Param("offset") int offset, @Param("pageSize") int pageSize);

  long countActions(@Param("gameId") long gameId);

  List<GameRow> findQueuedRobotGames(@Param("limit") int limit);

  int insertGame(GameRow row);

  int claimRobotTurn(@Param("id") long id, @Param("expectedVersion") int expectedVersion);

  int updateAfterAction(
      @Param("id") long id,
      @Param("expectedVersion") int expectedVersion,
      @Param("state") String state,
      @Param("status") String status,
      @Param("winnerSide") String winnerSide,
      @Param("robotStatus") String robotStatus);

  int clearActions(@Param("gameId") long gameId);

  int incrementHintCount(@Param("id") long id, @Param("expectedVersion") int expectedVersion);

  int restartGame(
      @Param("id") long id,
      @Param("expectedVersion") int expectedVersion,
      @Param("initialState") String initialState,
      @Param("robotStatus") String robotStatus,
      @Param("mode") String mode,
      @Param("updateBy") long updateBy);

  int abandonGame(
      @Param("id") long id,
      @Param("expectedVersion") int expectedVersion,
      @Param("updateBy") long updateBy);

  int markRobotFailed(@Param("id") long id, @Param("expectedVersion") int expectedVersion);

  int markRobotNeedsProof(@Param("id") long id, @Param("expectedVersion") int expectedVersion);

  int insertAction(GameActionRow row);
}
