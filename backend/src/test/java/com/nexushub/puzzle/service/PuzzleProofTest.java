package com.nexushub.puzzle.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexushub.analysis.service.AnalysisService;
import com.nexushub.identity.persistence.UserRow;
import com.nexushub.puzzle.api.PuzzleDtos.DetailRequest;
import com.nexushub.puzzle.api.PuzzleDtos.SearchRequest;
import com.nexushub.puzzle.persistence.PuzzleMapper;
import com.nexushub.puzzle.persistence.PuzzleRow;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 题库和详情必须一致展示证明结论，未证明时不能携带胜方。 */
class PuzzleProofTest {
  @Test
  void listAndDetailExposeBothWinnersAndUnknown() {
    PuzzleMapper mapper = mock(PuzzleMapper.class);
    PuzzleService service =
        new PuzzleService(new ObjectMapper(), mapper, mock(AnalysisService.class));
    UserRow actor = new UserRow();
    actor.setId(1L);
    actor.setRole("USER");
    PuzzleRow user = row(1L, "USER");
    PuzzleRow bot = row(2L, "BOT");
    PuzzleRow unknown = row(3L, null);
    when(mapper.search("PUBLISHED", 1L, null)).thenReturn(List.of(user, bot, unknown));
    var page = service.search(new SearchRequest(1, 20, "PUBLISHED", null, null, null, null), actor);
    assertEquals(
        List.of("PROVEN", "PROVEN", "UNKNOWN"),
        page.items().stream().map(p -> p.proofStatus()).toList());
    assertEquals("USER", page.items().get(0).winnerSide());
    assertEquals("BOT", page.items().get(1).winnerSide());
    assertNull(page.items().get(2).winnerSide());
    when(mapper.find(2L, null)).thenReturn(bot);
    assertEquals(page.items().get(1), service.detail(new DetailRequest("2", null), actor));
  }

  private PuzzleRow row(long id, String winner) {
    PuzzleRow row = new PuzzleRow();
    row.setId(id);
    row.setOwnerId(1L);
    row.setStatus("PUBLISHED");
    row.setRowVersion(1);
    row.setVersionNo(1);
    row.setVersionId(id + 100);
    row.setInitialStateJson("{}");
    row.setTagsJson("[]");
    row.setAllowedFirstSeatsJson("[0]");
    row.setWinnerSide(winner);
    return row;
  }
}
