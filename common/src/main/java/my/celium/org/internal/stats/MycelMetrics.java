package my.celium.org.internal.stats;

import com.google.gson.JsonObject;

import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.SimpleMetrics;
import my.celium.org.Mycel;
import my.celium.org.internal.PlatformHolder;
import my.celium.org.internal.ServerHolder;
import my.celium.org.platform.PlatformService;
import net.minecraft.server.MinecraftServer;

/** The default fields FastStats' own loader contexts send, filled in from Mycel's platform layer. */
final class MycelMetrics extends SimpleMetrics {
    private final boolean client;

    private MycelMetrics(Factory factory, boolean client) {
        super(factory);
        this.client = client;
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
        data.addProperty("plugin_version", Mycel.getVersion());
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

        Factory(SimpleContext context, boolean client) {
            super(context);
            this.client = client;
        }

        @Override
        public Metrics create() {
            return new MycelMetrics(this, client);
        }
    }
}
