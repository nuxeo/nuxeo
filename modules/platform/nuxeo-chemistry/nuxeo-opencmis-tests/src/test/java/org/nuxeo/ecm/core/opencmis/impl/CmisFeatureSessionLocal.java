/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.server.impl.CallContextImpl;
import org.apache.chemistry.opencmis.server.shared.TempStoreOutputStreamFactory;
import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisServiceFactory;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisServiceFactoryManager;
import org.nuxeo.ecm.core.opencmis.impl.client.NuxeoBinding;
import org.nuxeo.ecm.core.opencmis.impl.client.NuxeoSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Binder;

/**
 * Feature that starts an CMIS local session.
 */
public class CmisFeatureSessionLocal extends CmisFeatureSession {

    private static final int THRESHOLD = 4 * 1024 * 1024;

    private static final int MAX_SIZE = -1;


    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        super.configure(runner, binder);
        setLocal();
    }

    @Override
    public void beforeSetup(FeaturesRunner runner, FrameworkMethod method, Object test) throws Exception {
        String repositoryName = runner.getFeature(CoreFeature.class).getRepositoryName();
        setUpCmisSession(repositoryName);
    }

    @Override
    public void afterTeardown(FeaturesRunner runner, FrameworkMethod method, Object test) throws Exception {
        tearDownCmisSession();
    }

    @Override
    public Session setUpCmisSession(String repositoryName) {
        NuxeoCmisServiceFactoryManager manager = Framework.getService(NuxeoCmisServiceFactoryManager.class);
        NuxeoCmisServiceFactory serviceFactory = manager.getNuxeoCmisServiceFactory();
        TempStoreOutputStreamFactory streamFactory = TempStoreOutputStreamFactory.newInstance( //
                Environment.getDefault().getTemp(), THRESHOLD, MAX_SIZE, false);
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
