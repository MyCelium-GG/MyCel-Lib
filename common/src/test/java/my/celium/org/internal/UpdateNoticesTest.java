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
        assertEquals("Mycel › ↑ Updates available: unregistered_a 1.1.0, unregistered_b 2.0.1  see /mycel mods",
                TextUtil.plain(UpdateNotices.line(updates)));
    }
}
