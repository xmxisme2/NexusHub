package com.nexushub.analysis.persistence;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SolveTaskMapper {
  long nextTaskId();

  long nextResultId();

  SolveTaskRow findTask(@Param("id") long id);

  SolveResultRow findResult(@Param("taskId") long taskId);

  SolveTaskRow findByPuzzleVersion(
      @Param("puzzleVersionId") long puzzleVersionId, @Param("solverVersion") String solverVersion);

  List<SolveTaskRow> listQueued(@Param("limit") int limit);

  int insertTask(SolveTaskRow row);

  int claim(
      @Param("id") long id,
      @Param("leaseToken") String leaseToken,
      @Param("leaseSeconds") int leaseSeconds);

  int complete(
      @Param("id") long id,
      @Param("leaseToken") String leaseToken,
      @Param("terminationReason") String terminationReason,
      @Param("statsJson") String statsJson);

  int fail(
      @Param("id") long id,
      @Param("leaseToken") String leaseToken,
      @Param("errorCode") String errorCode,
      @Param("statsJson") String statsJson);

  int cancel(@Param("id") long id, @Param("ownerId") long ownerId);

  int insertResult(SolveResultRow row);

  List<SolveTaskRow> search(
      @Param("ownerId") long ownerId,
      @Param("status") String status,
      @Param("offset") int offset,
      @Param("pageSize") int pageSize);

  long count(@Param("ownerId") long ownerId, @Param("status") String status);
}
