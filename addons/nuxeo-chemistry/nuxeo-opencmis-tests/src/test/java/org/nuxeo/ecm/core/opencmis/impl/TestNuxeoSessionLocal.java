/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;


import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.apache.chemistry.opencmis.server.impl.CallContextImpl;
import org.nuxeo.ecm.core.opencmis.impl.client.NuxeoSession;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoCmisService;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoRepository;
import org.nuxeo.ecm.core.opencmis.tests.Helper;

/**
 * Test the high-level session using a local connection.
 */
public class TestNuxeoSessionLocal extends NuxeoSessionTestCase {

    @Override
    public void setUpCmisSession() throws Exception {
        boolean objectInfoRequired = true; // for tests
        CallContextImpl context = new CallContextImpl(
                CallContext.BINDING_LOCAL, getRepositoryId(),
                objectInfoRequired);
        context.put(CallContext.USERNAME, USERNAME);
        context.put(CallContext.PASSWORD, PASSWORD);
        context.put(CallContext.SERVLET_CONTEXT,
                FakeServletContext.getServletContext());
        NuxeoRepository repository = new NuxeoRepository(getRepositoryId(),
                getRootFolderId());
        session = new NuxeoSession(getCoreSession(), repository, context);
    }

    @Override
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
