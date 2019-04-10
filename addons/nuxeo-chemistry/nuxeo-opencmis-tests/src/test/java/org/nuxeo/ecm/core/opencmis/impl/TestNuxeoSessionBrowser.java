/*
 * Copyright (c) 2014 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.EventListener;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.server.impl.browser.CmisBrowserBindingServlet;
import org.junit.Ignore;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisContextListener;

/**
 * Test the high-level session using an JSON Browser connection.
 */
public class TestNuxeoSessionBrowser extends NuxeoSessionClientServerTestCase {

    @Override
    protected void addParams(Map<String, String> params) {
        params.put(SessionParameter.BINDING_TYPE, BindingType.BROWSER.value());
        params.put(SessionParameter.BROWSER_URL, serverURI.toString());
    }

    @Override
    protected Servlet getServlet() {
        return new CmisBrowserBindingServlet();
    }

    @Override
    protected Filter getFilter() {
        return new TrustingNuxeoAuthFilter();
    }

    @Override
    protected EventListener[] getEventListeners() {
        return new EventListener[] { new NuxeoCmisContextListener() };
    }

}
