package my.celium.org.diag;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import my.celium.org.Mycel;
import my.celium.org.TestPlatform;
import my.celium.org.internal.PlatformHolder;
import my.celium.org.metadata.ModMetadata;

class DiagnosticsTest {
    @BeforeAll
    static void initOnce(@TempDir Path tempDir) {
        PlatformHolder.setForTesting(new TestPlatform(tempDir));
        Mycel.initialize();
        Mycel.registerMod(ModMetadata.builder("diagdemo", "Diag Demo", "3.2.1").build());
    }

    @Test
    void summaryMentionsLibraryAndMods() {
        List<String> lines = MycelDiagnostics.summaryLines();
        assertTrue(lines.stream().anyMatch(line -> line.contains("Mycel " + my.celium.org.Mycel.getVersion())),
                "expected library line in " + lines);
        assertTrue(lines.stream().anyMatch(line -> line.contains("diagdemo") && line.contains("3.2.1")),
                "expected mod line in " + lines);
    }
}
