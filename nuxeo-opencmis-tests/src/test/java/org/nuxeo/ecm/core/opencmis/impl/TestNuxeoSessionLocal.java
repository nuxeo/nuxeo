/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.server.impl.CallContextImpl;
import org.apache.chemistry.opencmis.server.shared.ThresholdOutputStreamFactory;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisServiceFactory;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisServiceFactoryManager;
import org.nuxeo.ecm.core.opencmis.impl.client.NuxeoBinding;
import org.nuxeo.ecm.core.opencmis.impl.client.NuxeoSession;
import org.nuxeo.runtime.api.Framework;

/**
 * Test the high-level session using a local connection.
 */
public class TestNuxeoSessionLocal extends NuxeoSessionTestCase {

    private static final int THRESHOLD = 4 * 1024 * 1024;

    private static final int MAX_SIZE = -1;

    @Override
    public void setUpCmisSession() throws Exception {
        setUpCmisSession(USERNAME);
    }

    @Override
    protected void setUpCmisSession(String username) throws Exception {
        NuxeoCmisServiceFactoryManager manager = Framework.getService(NuxeoCmisServiceFactoryManager.class);
        NuxeoCmisServiceFactory serviceFactory = manager.getNuxeoCmisServiceFactory();
        ThresholdOutputStreamFactory streamFactory = ThresholdOutputStreamFactory.newInstance(
                new File(System.getProperty("java.io.tmpdir")),
                THRESHOLD, MAX_SIZE, false);
        HttpServletRequest request = null;
        HttpServletResponse response = null;
        CallContextImpl context = new CallContextImpl(
                CallContext.BINDING_LOCAL, CmisVersion.CMIS_1_1, getRepositoryId(),
                FakeServletContext.getServletContext(), request, response,
                serviceFactory, streamFactory);
        context.put(CallContext.USERNAME, username);
        context.put(CallContext.PASSWORD, PASSWORD);
        CmisService service = serviceFactory.getService(context);
        NuxeoBinding binding = new NuxeoBinding(service);
        session = new NuxeoSession(binding, context);
    }

    @Override
    public void tearDownCmisSession() throws Exception {
        if (session != null) {
            session.getBinding().close();
            session.clear();
            session = null;
        }
    }

}
