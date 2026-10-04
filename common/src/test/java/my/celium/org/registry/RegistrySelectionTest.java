package my.celium.org.registry;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import my.celium.org.metadata.ModMetadata;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/**
 * Proves bridge selection picks the runtime-matching implementation: the
 * lazy (NeoForge-style) stub must win over the eager (Fabric-style) one in
 * this ModDev environment, so the factory must never run.
 */
class RegistrySelectionTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @AfterEach
    void clean() {
        MycelRegistry.RegistryBridges.resetForTesting();
    }

    @Test
    void selectsLazyBridgeForDetectedRuntime() {
        MycelRegistry.RegistryBridges.resetForTesting();
        AtomicBoolean ran = new AtomicBoolean(false);
        ModMetadata meta = ModMetadata.builder("seltest", "Sel Test", "1.0.0").build();
        MycelRegistry.items(meta).register("test_path", () -> {
            ran.set(true);
            return null;
        });
        assertFalse(ran.get(), "wrong bridge ran the factory eagerly");
    }
}
