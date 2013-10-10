/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Vladimir Pasquier <vpasquier@nuxeo.com>
 * Stephane Lacoin <slacoin@nuxeo.com>
 */

package org.nuxeo.ecm.automation.client.jaxrs.impl;

import java.net.URISyntaxException;
import java.net.URL;

import org.nuxeo.ecm.automation.client.AutomationClient;
import org.nuxeo.ecm.automation.client.AutomationClientFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @since 5.7 Automation client osgi activator to use HttpAutomationClient
 *        service
 */
public class AutomationClientActivator implements AutomationClientFactory,
        BundleActivator {

    protected static volatile AutomationClientActivator instance;

    protected BundleContext context;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        bundleContext.registerService(AutomationClientFactory.class.getName(),
                this, null);
        this.instance = this;
        this.context = bundleContext;
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        this.instance = null;
        this.context = null;
    }

    @Override
    public AutomationClient getClient(URL url) throws URISyntaxException {
        return new HttpAutomationClient(url.toURI().toASCIIString());
    }

    public BundleContext getContext() {
        return context;
    }

    @Override
    public AutomationClient getClient(URL url, int httpCxTimeout)
            throws URISyntaxException {
        return new HttpAutomationClient(url.toURI().toASCIIString(),
                httpCxTimeout);
    }

    public static AutomationClientActivator getInstance() {
        return instance;
    }
}
