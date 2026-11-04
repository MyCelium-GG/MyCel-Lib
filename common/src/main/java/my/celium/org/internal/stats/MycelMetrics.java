package my.celium.org.internal.stats;

import com.google.gson.JsonObject;

import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import my.celium.org.internal.PlatformHolder;
import my.celium.org.internal.ServerHolder;
import my.celium.org.platform.PlatformService;
import net.minecraft.server.MinecraftServer;

/** The default fields FastStats' own loader contexts send, filled in from Mycel's platform layer. */
final class MycelMetrics extends SimpleMetrics {
    private final boolean client;
    private final String version;

    private MycelMetrics(Factory factory, boolean client, String version) {
        super(factory);
        this.client = client;
        this.version = version;
    }

    @Override
    public boolean isClientApplication() {
        return client;
    }

    @Override
    protected String serverType() {
        return PlatformHolder.get().platform().displayName();
    }

    @Override
    protected void appendDefaultData(JsonObject data) {
        PlatformService platform = PlatformHolder.get();
        data.addProperty("plugin_version", version);
        data.addProperty("server_type", platform.platform().displayName());
        data.addProperty("platform_version", platform.loaderVersion());
        data.addProperty("game_version", platform.minecraftVersion());
        MinecraftServer server = ServerHolder.get();
        if (!client && server != null) {
            data.addProperty("player_count", server.getPlayerCount());
            data.addProperty("online_mode", server.usesAuthentication());
        }
    }

    static final class Factory extends SimpleMetrics.Factory {
        private final boolean client;
        private final String version;

        Factory(SimpleContext context, boolean client, String version) {
            super(context);
            this.client = client;
            this.version = version;
        }

        @Override
        public Metrics create() {
            return new MycelMetrics(this, client, version);
        }
    }
}
