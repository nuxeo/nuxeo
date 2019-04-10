/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.server.impl.CallContextImpl;
import org.apache.chemistry.opencmis.server.shared.ThresholdOutputStreamFactory;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisServiceFactory;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisServiceFactoryManager;
import org.nuxeo.ecm.core.opencmis.impl.client.NuxeoBinding;
import org.nuxeo.ecm.core.opencmis.impl.client.NuxeoSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.CoreScope;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Binder;
import com.google.inject.Provider;

/**
 * Feature that starts an CMIS local session.
 */
public class CmisFeatureSessionLocal extends CmisFeatureSession {

    private static final int THRESHOLD = 4 * 1024 * 1024;

    private static final int MAX_SIZE = -1;

    public Session session;

    public class SessionProvider implements Provider<Session> {
        @Override
        public Session get() {
            return session;
        }
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        super.configure(runner, binder);
        Provider<Session> sessionProvider = new SessionProvider();
        binder.bind(Session.class).toProvider(sessionProvider).in(CoreScope.INSTANCE);
        setLocal();
    }

    @Override
    public void beforeSetup(FeaturesRunner runner) throws Exception {
        String repositoryName = runner.getFeature(CoreFeature.class).getRepository().getName();
        setUpCmisSession(repositoryName);
    }

    @Override
    public void afterTeardown(FeaturesRunner runner) throws Exception {
        tearDownCmisSession();
    }

    @Override
    public Session setUpCmisSession(String repositoryName) {
        NuxeoCmisServiceFactoryManager manager = Framework.getService(NuxeoCmisServiceFactoryManager.class);
        NuxeoCmisServiceFactory serviceFactory = manager.getNuxeoCmisServiceFactory();
        ThresholdOutputStreamFactory streamFactory = ThresholdOutputStreamFactory.newInstance(
                new File(System.getProperty("java.io.tmpdir")), THRESHOLD, MAX_SIZE, false);
        HttpServletRequest request = null;
        HttpServletResponse response = null;
        CallContextImpl context = new CallContextImpl(CallContext.BINDING_LOCAL, CmisVersion.CMIS_1_1, repositoryName,
                FakeServletContext.getServletContext(), request, response, serviceFactory, streamFactory);
        context.put(CallContext.USERNAME, USERNAME);
        context.put(CallContext.PASSWORD, PASSWORD);
        CmisService service = serviceFactory.getService(context);
        NuxeoBinding binding = new NuxeoBinding(service);
        session = new NuxeoSession(binding, context);
        return session;
    }

    @Override
    public void tearDownCmisSession() {
        if (session != null) {
            session.getBinding().close();
            session.clear();
            session = null;
        }
    }

}
