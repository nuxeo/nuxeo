/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN <stan@nuxeo.com>
 *     vpasquier <vpasquier@nuxeo.com>
 *     slacoin <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.automation.test;

import org.nuxeo.ecm.automation.OperationCallback;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.trace.TracerFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.RepositorySettings;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import com.google.inject.Binder;
import com.google.inject.Provider;

/**
 * Based on the existing {@link PlatformFeature}, AutomationFeature is a simple
 * feature that includes org.nuxeo.ecm.automation.core and
 * org.nuxeo.ecm.automation.features bundles.
 *
 * @since 5.7
 * @since 5.6-HF02
 *
 */
@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.features",
        "org.nuxeo.ecm.platform.query.api", "org.nuxeo.runtime.management" })
public class AutomationFeature extends SimpleFeature {

    protected final OperationContextProvider contextProvider = new OperationContextProvider();

    protected final TracerProvider tracerProvider = new TracerProvider();

    protected OperationContext context;

    protected TracerFactory tracerFactory;

    protected OperationCallback tracer;

    protected RepositorySettings repository;

    public class OperationContextProvider implements Provider<OperationContext> {

        @Override
        public OperationContext get() {
            return AutomationFeature.this.getContext();
        }

    }

    class TracerProvider implements Provider<OperationCallback> {

        @Override
        public OperationCallback get() {
            return AutomationFeature.this.getTracer();
        }

    }

    protected OperationContext getContext() {
        if (context == null) {
            CoreSession session = repository.getSession();
            context = new OperationContext(session);
            if (tracer != null) {
                context.addChainCallback(tracer);
            }
        }
        return context;
    }

    protected OperationCallback getTracer() {
        if (tracer == null) {
            tracer = tracerFactory.newTracer("*");
            if (context != null) {
                context.addChainCallback(tracer);
            }
        }
        return tracer;
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        binder.bind(OperationContext.class).toProvider(contextProvider).in(
                AutomationScope.INSTANCE);
        binder.bind(OperationCallback.class).toProvider(tracerProvider).in(
                AutomationScope.INSTANCE);
        repository = runner.getFeature(CoreFeature.class).getRepository();
        tracerFactory = Framework.getLocalService(TracerFactory.class);
    }

    @Override
    public void beforeSetup(FeaturesRunner runner) throws Exception {
        AutomationScope.INSTANCE.enter();
    }

    @Override
    public void afterTeardown(FeaturesRunner runner) throws Exception {
        AutomationScope.INSTANCE.exit();
        context = null;
        tracer = null;
        tracerFactory.clearTraces();
    }
}
