package com.nexushub.puzzle.persistence;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PuzzleMapper {
  List<PuzzleRow> search(
      @Param("scope") String scope,
      @Param("ownerId") long ownerId,
      @Param("keyword") String keyword);

  PuzzleRow find(@Param("id") long id, @Param("versionId") Long versionId);

  PuzzleRow findIncludingDeleted(@Param("id") long id, @Param("versionId") Long versionId);

  PuzzleRow findDuplicate(
      @Param("stateJson") String stateJson, @Param("excludePuzzleId") Long excludePuzzleId);

  PuzzleRow findByVersionId(@Param("versionId") long versionId);

  List<PuzzleRow> listPublishedVersions();

  long nextPuzzleId();

  long nextVersionId();

  int insertPuzzle(@Param("id") long id, @Param("ownerId") long ownerId);

  int insertVersion(PuzzleRow row);

  int updateDraft(
      @Param("id") long id,
      @Param("versionId") long versionId,
      @Param("expectedRowVersion") int expectedRowVersion);

  int publish(
      @Param("id") long id,
      @Param("versionId") long versionId,
      @Param("expectedRowVersion") int expectedRowVersion);

  int archive(@Param("id") long id, @Param("expectedRowVersion") int expectedRowVersion);

  int restore(@Param("id") long id, @Param("expectedRowVersion") int expectedRowVersion);
}
