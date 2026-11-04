package my.celium.org.internal.stats;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import dev.faststats.Metrics;
import dev.faststats.SimpleContext;
import dev.faststats.config.SimpleConfig;
import dev.faststats.internal.LoggerFactory;

/**
 * FastStats context for Mycel and the mods using {@link my.celium.org.stats.ModStats}. FastStats ships contexts per loader, but Mycel is one jar for
 * every loader, so this one runs on Mycel's own thread and asks Mycel for platform details.
 * Clients only create it after the player said yes on the consent screen; dedicated servers
 * follow FastStats' first-run rule (a notice in the console, nothing sent on the first start).
 */
final class MycelStatsContext extends SimpleContext {
    private final boolean client;
    private final String project;
    private final String version;
    private final ScheduledExecutorService executor;

    private MycelStatsContext(Factory factory) {
        super(factory, factory.loggers, factory.config, factory.platform, factory.token);
        this.client = factory.client;
        this.project = factory.project;
        this.version = factory.version;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Mycel stats (" + factory.project + ")");
            thread.setDaemon(true);
            return thread;
        });
        initializeManagedServices(factory);
    }

    @Override
    protected boolean preSubmissionStart() {
        return client || ((SimpleConfig) getConfig()).preSubmissionStart(this);
    }

    @Override
    public String getProjectName() {
        return project;
    }

    @Override
    protected Metrics.Factory metricsFactory() {
        return new MycelMetrics.Factory(this, client, version);
    }

    @Override
    protected void scheduleAtFixedRate(Runnable task, long delay, long period, TimeUnit unit) {
        executor.scheduleAtFixedRate(task, delay, period, unit);
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
        } finally {
            executor.shutdownNow();
        }
    }

    static final class Factory extends SimpleContext.Factory<MycelStatsContext, Factory> {
        private final LoggerFactory loggers;
        private final SimpleConfig config;
        private final String platform;
        private final String token;
        private final boolean client;
        private final String project;
        private final String version;

        Factory(LoggerFactory loggers, SimpleConfig config, String platform, String token, boolean client,
                String project, String version) {
            this.loggers = loggers;
            this.config = config;
            this.platform = platform;
            this.token = token;
            this.client = client;
            this.project = project;
            this.version = version;
        }

        @Override
        public MycelStatsContext create() {
            return new MycelStatsContext(this);
        }
    }
}
