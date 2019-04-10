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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.apache.chemistry.opencmis.server.impl.CallContextImpl;
import org.apache.chemistry.opencmis.server.shared.ThresholdOutputStreamFactory;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisServiceFactory;
import org.nuxeo.ecm.core.opencmis.impl.client.NuxeoSession;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoCmisService;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoRepository;
import org.nuxeo.ecm.core.opencmis.tests.Helper;

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
        NuxeoRepository repository = new NuxeoRepository(getRepositoryId(),
                getRootFolderId());
        session = new NuxeoSession(getCoreSession(), repository, context);
    }

    @Override
    @After
    public void tearDownCmisSession() throws Exception {
        session = null;
    }

    protected String createDocumentMyDocType() {
        NuxeoCmisService service = ((NuxeoSession) session).getService();
        BindingsObjectFactory factory = service.getObjectFactory();
        List<PropertyData<?>> props = new ArrayList<PropertyData<?>>();
        props.add(factory.createPropertyIdData(PropertyIds.NAME, "My Title"));
        props.add(factory.createPropertyIdData(PropertyIds.OBJECT_TYPE_ID,
                "MyDocType"));
        props.add(factory.createPropertyStringData("my:string", "abc"));
        props.add(factory.createPropertyBooleanData("my:boolean", Boolean.TRUE));
        props.add(factory.createPropertyIntegerData("my:integer",
                BigInteger.valueOf(123)));
        props.add(factory.createPropertyIntegerData("my:long",
                BigInteger.valueOf(123)));
        props.add(factory.createPropertyDecimalData("my:double",
                BigDecimal.valueOf(123.456)));
        GregorianCalendar expectedDate = Helper.getCalendar(2010, 9, 30, 16, 4,
                55);
        props.add(factory.createPropertyDateTimeData("my:date", expectedDate));
        Properties properties = factory.createPropertiesData(props);
        String id = service.createDocument(getRepositoryId(), properties,
                rootFolderId, null, null, null, null, null, null);
        assertNotNull(id);
        return id;
    }
}
