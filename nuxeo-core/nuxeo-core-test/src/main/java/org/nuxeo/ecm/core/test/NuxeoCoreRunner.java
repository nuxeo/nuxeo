/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.core.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.ecm.core.test.guice.CoreModule;
import org.nuxeo.runtime.test.runner.NuxeoRunner;
import org.nuxeo.runtime.test.runner.RuntimeModule;

import com.google.inject.Module;
import com.google.inject.Provider;

/**
 * JUnit4 runner that provide facilities to setup {@link CoreSession}-based
 * tests.
 */
public class NuxeoCoreRunner extends NuxeoRunner implements
        Provider<BackendType> {

    private static final Log log = LogFactory.getLog(NuxeoCoreRunner.class);

    private Settings settings;

    private BackendType backendType;

    private final static Stack<CoreSession> injectedSessions = new Stack<CoreSession>();

    public NuxeoCoreRunner(Class<?> classToRun) throws InitializationError {
        this(classToRun, new RuntimeModule(), new CoreModule());
    }

    public NuxeoCoreRunner(Class<?> classToRun, Module... modules)
            throws InitializationError {
        super(classToRun, modules);
        settings = new Settings(getDescription());
    }

    public void setBackendType(BackendType backendType) {
        this.backendType = backendType;
    }

    public static Settings getSettings() {
        return ((NuxeoCoreRunner) getInstance()).settings;
    }

    @Override
    protected void beforeRun() {
        if (settings.getRepositoryCleanupGranularity() == Granularity.CLASS) {
            cleanupSession();
        }
    }

    @Override
    protected void afterRun() {
        List<String> repoNames = new ArrayList<String>();

        if (injectedSessions.size() > 0) {

            while (injectedSessions.size() > 0) {
                CoreSession session = injectedSessions.pop();

                if (!repoNames.contains(session.getRepositoryName())) {
                    repoNames.add(session.getRepositoryName());
                }

                try {
                    CoreInstance.getInstance().close(session);
                } catch (Exception e) {
                    log.error("Unable to close session: " + e.getMessage(), e);
                }
            }
        }

        for (String repoName : repoNames) {
            try {
                Repository repository = NXCore.getRepositoryService()
                        .getRepositoryManager().getRepository(repoName);
                log.info("Shutdown repository : " + repoName);
                repository.shutdown();
            } catch (Exception e) {
                log.error("Unable to get repository : " + repoName,e);
            }
        }

    }

    @Override
    protected Statement methodInvoker(FrameworkMethod method, Object test) {
        Statement statement = super.methodInvoker(method, test);
        if (settings.getRepositoryCleanupGranularity() == Granularity.METHOD) {
            cleanupSession();
        }
        return statement;
    }

    protected void cleanupSession() {
        CoreSession session = injector.getInstance(CoreSession.class);

        try {
            session.removeChildren(new PathRef("/"));
        } catch (ClientException e) {
            log.error("Unable to reset repository", e);
        }
        RepositoryInit factory = settings.getRepositoryInitializer();
        if (factory != null) {
            try {
                factory.populate(session);
                session.save();
            } catch (ClientException e) {
                log.error(e.toString(), e);
            }
        }

    }

    public BackendType get() {
        if (backendType == null) {
            // backend type is specified by the test class
            return settings.getBackendType();
        } else {
            // multi-backend case, return current one
            return backendType;
        }
    }

    // Waiting for Guice 2.0 type listeners
    public static void onSessionInjected(CoreSession session) {
        if (!injectedSessions.contains(session)) {
            injectedSessions.push(session);
        }
    }

}
