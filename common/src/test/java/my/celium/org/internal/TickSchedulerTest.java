package my.celium.org.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import my.celium.org.schedule.ScheduledTask;

class TickSchedulerTest {
    @AfterEach
    void clean() {
        TickScheduler.clear();
    }

    @Test
    void runLaterFiresAfterDelay() {
        List<String> events = new ArrayList<>();
        TickScheduler.runLater(2, () -> events.add("fired"));
        TickScheduler.tick();
        assertTrue(events.isEmpty());
        TickScheduler.tick();
        assertEquals(List.of("fired"), events);
    }

    @Test
    void runEveryRepeatsUntilCancelled() {
        List<Long> ticks = new ArrayList<>();
        ScheduledTask task = TickScheduler.runEvery(2, () -> ticks.add(TickScheduler.currentTick()));
        for (int i = 0; i < 5; i++) {
            TickScheduler.tick();
        }
        assertEquals(2, ticks.size());
        assertFalse(task.isCancelled());
        task.cancel();
        assertTrue(task.isCancelled());
        for (int i = 0; i < 4; i++) {
            TickScheduler.tick();
        }
        assertEquals(2, ticks.size());
    }

    @Test
    void throwingTasksDoNotBreakTheQueue() {
        List<String> events = new ArrayList<>();
        TickScheduler.runLater(0, () -> {
            throw new RuntimeException("boom");
        });
        TickScheduler.runLater(0, () -> events.add("second"));
        TickScheduler.tick();
        assertEquals(List.of("second"), events);
    }

    @Test
    void clearDropsEverything() {
        List<String> events = new ArrayList<>();
        TickScheduler.runLater(1, () -> events.add("late"));
        TickScheduler.clear();
        TickScheduler.tick();
        TickScheduler.tick();
        assertTrue(events.isEmpty());
    }
}
