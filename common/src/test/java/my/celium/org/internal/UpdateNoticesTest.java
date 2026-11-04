package my.celium.org.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import my.celium.org.update.UpdateResult;
import my.celium.org.util.TextUtil;

class UpdateNoticesTest {
    @Test
    void oneLineListsEveryUpdate() {
        List<UpdateResult> updates = List.of(
                new UpdateResult("unregistered_a", "1.0.0", "1.1.0", null, true, 0L),
                new UpdateResult("unregistered_b", "2.0.0", "2.0.1", null, true, 0L));
        List<String> lines = UpdateNotices.lines(updates).stream().map(TextUtil::plain).toList();
        assertEquals(List.of("Mycel \u00BB Updates ready   Show all",
                "\u203A unregistered_a  1.0.0 \u2192 1.1.0",
                "\u203A unregistered_b  2.0.0 \u2192 2.0.1"), lines);
    }
}
