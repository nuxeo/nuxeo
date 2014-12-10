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

import java.util.EventListener;
import java.util.Map;

import javax.servlet.Servlet;

import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisBrowserBindingServlet;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisContextListener;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Binder;

/**
 * Feature that starts a Browser Binding session.
 */
public class CmisFeatureSessionBrowser extends CmisFeatureSessionHttp {

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        super.configure(runner, binder);
        setBrowser();
    }

    @Override
    protected void addParams(Map<String, String> params) {
        params.put(SessionParameter.BINDING_TYPE, BindingType.BROWSER.value());
        params.put(SessionParameter.BROWSER_URL, serverURI.toString());
    }

    @Override
    protected Servlet getServlet() {
        return new NuxeoCmisBrowserBindingServlet();
    }

    @Override
    protected EventListener[] getEventListeners() {
        return new EventListener[] { new NuxeoCmisContextListener() };
    }

}
