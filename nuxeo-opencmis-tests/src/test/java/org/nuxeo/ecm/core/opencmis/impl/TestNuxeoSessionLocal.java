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
import org.apache.chemistry.opencmis.server.impl.CallContextImpl;
import org.apache.chemistry.opencmis.server.shared.ThresholdOutputStreamFactory;
import org.junit.After;
import org.junit.Before;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisServiceFactory;
import org.nuxeo.ecm.core.opencmis.impl.client.NuxeoSession;

/**
 * Test the high-level session using a local connection.
 */
public class TestNuxeoSessionLocal extends NuxeoSessionTestCase {

    private static final int THRESHOLD = 4 * 1024 * 1024;

    private static final int MAX_SIZE = -1;

    @Override
    @Before
    public void setUpCmisSession() throws Exception {
        setUpCmisSession(USERNAME);
    }

    @Override
    protected void setUpCmisSession(String username) throws Exception {
        ThresholdOutputStreamFactory streamFactory = ThresholdOutputStreamFactory.newInstance(
                new File((String) System.getProperty("java.io.tmpdir")),
                THRESHOLD, MAX_SIZE, false);
        HttpServletRequest request = null;
        HttpServletResponse response = null;
        CallContextImpl context = new CallContextImpl(
                CallContext.BINDING_LOCAL, CmisVersion.CMIS_1_1, getRepositoryId(),
                FakeServletContext.getServletContext(), request, response,
                new NuxeoCmisServiceFactory(), streamFactory);
        context.put(CallContext.USERNAME, username);
        context.put(CallContext.PASSWORD, PASSWORD);
        session = new NuxeoSession(getCoreSession(), context);
    }

    @Override
    @After
    public void tearDownCmisSession() throws Exception {
        session = null;
    }

}
