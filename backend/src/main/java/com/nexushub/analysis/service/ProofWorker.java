package com.nexushub.analysis.service;

import com.nexushub.analysis.persistence.SolveTaskMapper;
import com.nexushub.analysis.persistence.SolveTaskRow;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 持久任务 Worker；固定最多 5 个证明线程，超过上限继续留在 QUEUED。 */
@Component
public class ProofWorker {
  private final SolveTaskMapper tasks;
  private final AnalysisService analysis;
  private final Set<Long> running = ConcurrentHashMap.newKeySet();
  private final ExecutorService executor;

  public ProofWorker(
      SolveTaskMapper tasks,
      AnalysisService analysis,
      @Value("${nexushub.solver.max-concurrency:5}") int concurrency) {
    this.tasks = tasks;
    this.analysis = analysis;
    int max = Math.max(1, Math.min(5, concurrency));
    AtomicInteger seq = new AtomicInteger();
    ThreadFactory f =
        r -> {
          Thread t = new Thread(r, "nexushub-proof-" + seq.incrementAndGet());
          t.setDaemon(true);
          return t;
        };
    executor = Executors.newFixedThreadPool(max, f);
  }

  @Scheduled(fixedDelayString = "${nexushub.solver.dispatch-delay-ms:500}")
  public void dispatch() {
    int slots = 5 - running.size();
    if (slots <= 0) return;
    for (SolveTaskRow row : tasks.listQueued(slots)) {
      if (running.size() >= 5) break;
      String token = UUID.randomUUID().toString().replace("-", "");
      if (tasks.claim(row.getId(), token, 120) != 1) continue;
      running.add(row.getId());
      executor.submit(
          () -> {
            try {
              analysis.execute(row, token);
            } finally {
              running.remove(row.getId());
            }
          });
    }
  }
}
