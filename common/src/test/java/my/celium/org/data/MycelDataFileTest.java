package my.celium.org.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonObject;

import my.celium.org.metadata.ModMetadata;

class MycelDataFileTest {
    private static final ModMetadata META = ModMetadata.builder("datatest", "Data Test", "1.0.0").build();

    static final class Root {
        Map<String, Integer> highlights = new LinkedHashMap<>();
    }

    @Test
    void missingFileGivesFallback(@TempDir Path dir) {
        MycelDataFile file = MycelDataFile.at(META, dir.resolve("data.json"));
        assertFalse(file.exists());
        assertTrue(file.read(Root.class, Root::new).highlights.isEmpty());
        assertEquals(0, file.readObject().size());
    }

    @Test
    void roundTripsObjects(@TempDir Path dir) {
        MycelDataFile file = MycelDataFile.at(META, dir.resolve("nested/data.json"));
        Root root = new Root();
        root.highlights.put("minecraft:diamond", 0x55FFFF);
        assertTrue(file.write(root));
        assertEquals(0x55FFFF, file.read(Root.class, Root::new).highlights.get("minecraft:diamond"));
        assertFalse(Files.exists(dir.resolve("nested/data.json.tmp")));
    }

    @Test
    void brokenFileIsKeptAsideNotOverwritten(@TempDir Path dir) throws Exception {
        Path path = dir.resolve("data.json");
        Files.writeString(path, "{ \"highlights\": { oops", StandardCharsets.UTF_8);
        MycelDataFile file = MycelDataFile.at(META, path);

        assertTrue(file.read(Root.class, Root::new).highlights.isEmpty());
        Path broken = dir.resolve("data.json.broken");
        assertTrue(Files.exists(broken));
        assertEquals("{ \"highlights\": { oops", Files.readString(broken));

        // Saving afterwards must not touch the broken copy.
        assertTrue(file.write(new Root()));
        assertEquals("{ \"highlights\": { oops", Files.readString(broken));
    }

    @Test
    void secondBrokenFileGetsItsOwnName(@TempDir Path dir) throws Exception {
        Path path = dir.resolve("data.json");
        MycelDataFile file = MycelDataFile.at(META, path);
        Files.writeString(path, "not json one");
        file.readObject();
        Files.writeString(path, "not json two");
        file.readObject();
        try (var listing = Files.list(dir)) {
            assertEquals(2, listing.filter(p -> p.getFileName().toString().startsWith("data.json.broken")).count());
        }
    }

    @Test
    void wrongShapeAndOversizeAreQuarantined(@TempDir Path dir) throws Exception {
        Path path = dir.resolve("list.json");
        Files.writeString(path, "[1, 2, 3]");
        JsonObject root = MycelDataFile.at(META, path).readObject();
        assertEquals(0, root.size());
        assertTrue(Files.exists(dir.resolve("list.json.broken")));

        Path big = dir.resolve("big.json");
        Files.writeString(big, "{\"a\": \"" + "x".repeat(200) + "\"}");
        MycelDataFile.at(META, big).withMaxBytes(100).readObject();
        assertTrue(Files.exists(dir.resolve("big.json.broken")));
    }

    @Test
    void invalidUtf8IsQuarantined(@TempDir Path dir) throws Exception {
        Path path = dir.resolve("bytes.json");
        Files.write(path, new byte[] {'{', (byte) 0xC3, (byte) 0x28, '}'});
        MycelDataFile.at(META, path).readObject();
        assertTrue(Files.exists(dir.resolve("bytes.json.broken")));
    }

    @Test
    void explicitQuarantineAndDelete(@TempDir Path dir) throws Exception {
        MycelDataFile file = MycelDataFile.at(META, dir.resolve("data.json"));
        assertTrue(file.write(new Root()));
        assertNotNull(file.quarantine("failed validation"));
        assertFalse(file.exists());
        assertTrue(file.write(new Root()));
        assertTrue(file.delete());
        assertFalse(file.exists());
    }

    @Test
    void rejectsPathsInFileNames() {
        assertThrows(IllegalArgumentException.class, () -> MycelDataFile.inConfigDir(META, "../evil.json"));
        assertThrows(IllegalArgumentException.class, () -> MycelDataFile.inConfigDir(META, ""));
    }
}
