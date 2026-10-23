package my.celium.org.diag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MycelDumpTest {
    @Test
    void redactsAddressesButKeepsLoopbackAndVersions() {
        String out = MycelDump.redact("Steve[/203.0.113.42:51234] logged in; bound to 127.0.0.1 and 0.0.0.0, NeoForge 26.3.0.1-beta, mod v1.0.2.3");
        assertFalse(out.contains("203.0.113.42"), out);
        assertTrue(out.contains("<ip>:51234"), out);
        assertTrue(out.contains("127.0.0.1"), out);
        assertTrue(out.contains("0.0.0.0"), out);
        assertTrue(out.contains("26.3.0.1-beta"), out);
        assertTrue(out.contains("v1.0.2.3"), out);
    }

    @Test
    void redactsIpv6() {
        String out = MycelDump.redact("from /2001:db8:85a3:0:0:8a2e:370:7334 at 12:34:56");
        assertFalse(out.contains("2001:db8"), out);
        assertTrue(out.contains("12:34:56"), out);
    }

    @Test
    void redactsTokens() {
        String out = MycelDump.redact("--username Steve --accessToken eyJhbGciOi.abc.def --version 26.3");
        assertFalse(out.contains("eyJhbGciOi"), out);
        assertTrue(out.contains("--accessToken <redacted>"), out);
        assertTrue(out.contains("--version 26.3"), out);
    }

    @Test
    void redactsHomeFolder() {
        String home = System.getProperty("user.home");
        String out = MycelDump.redact("Loading " + home + "/.minecraft/mods/x.jar");
        assertEquals("Loading ~/.minecraft/mods/x.jar", out);
    }

    @Test
    void readsPasteKeyFromBodyOrLocation() {
        assertEquals("6IvVPnXHVn", MycelDump.keyOf("{\"key\":\"6IvVPnXHVn\"}", null));
        assertEquals("abc123", MycelDump.keyOf("not json", "abc123"));
        assertNull(MycelDump.keyOf("{\"key\":\"../evil\"}", null));
        assertNull(MycelDump.keyOf("", null));
    }

    @Test
    void tailStartsAtWholeLine(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("latest.log");
        Files.writeString(file, "first line\nsecond line\nthird\n", StandardCharsets.UTF_8);
        assertEquals("third\n", MycelDump.tail(file, 10));
        assertEquals("first line\nsecond line\nthird\n", MycelDump.tail(file, 1000));
    }
}
