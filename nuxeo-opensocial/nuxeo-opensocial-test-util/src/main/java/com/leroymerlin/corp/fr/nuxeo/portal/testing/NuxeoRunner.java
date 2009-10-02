package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Module;

public class NuxeoRunner extends GuiceRunner {

    protected static TestRuntimeHarness harness = new TestRuntimeHarness();
    private static final Logger LOG = LoggerFactory
            .getLogger(NuxeoRunner.class);

    private Settings settings;

    private static NuxeoRunner currentInstance;
    private RepoType type = null;

    public NuxeoRunner(Class<?> classToRun) throws InitializationError {
        this(classToRun, new NuxeoModule());
    }

    public NuxeoRunner(Class<?> classToRun, Module... modules)
            throws InitializationError {
        super(classToRun, modules);
        settings = new Settings(getDescription());
        currentInstance = this;
    }

    public static NuxeoRunner getInstance() {
        return currentInstance;
    }

    public Settings getSettings() {
        return settings;
    }

    public static TestRuntimeHarness getRuntimeHarness() throws Exception {
        if (!harness.isStarted()) {
            harness.start(false);
        }
        return harness;
    }

    @Override
    public void run(final RunNotifier notifier) {

        RepoFactory factory = settings.getRepoFactory();
        if (factory != null) {
            CoreSession session = injector.getInstance(CoreSession.class);
            try {
                // TODO: run it in a UnrestrictedSessionRunner
                factory.createRepo(session);
            } catch (ClientException e) {
                e.printStackTrace();
            }
        }

        try {
            super.run(notifier);
            if (harness.isStarted()) {
                harness.stop();
            }
        } catch (Exception e) {
            notifier.fireTestFailure(new Failure(getDescription(), e));
        }

    }

    public void setRepoType(RepoType type) {
        this.type = type;
    }

    public RepoType getRepoType() {
        if (this.type == null) {
            return settings.getRepoType();
        } else {
            return this.type;
        }
    }

}
