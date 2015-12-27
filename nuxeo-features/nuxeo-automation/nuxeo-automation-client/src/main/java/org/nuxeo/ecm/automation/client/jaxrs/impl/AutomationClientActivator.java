/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 * @since 5.7 Automation client osgi activator to use HttpAutomationClient service
 */
public class AutomationClientActivator implements AutomationClientFactory, BundleActivator {

    protected static volatile AutomationClientActivator instance;

    protected BundleContext context;

    @Override
    public void start(BundleContext bundleContext) {
        bundleContext.registerService(AutomationClientFactory.class.getName(), this, null);
        this.instance = this;
        this.context = bundleContext;
    }

    @Override
    public void stop(BundleContext bundleContext) {
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
    public AutomationClient getClient(URL url, int httpCxTimeout) throws URISyntaxException {
        return new HttpAutomationClient(url.toURI().toASCIIString(), httpCxTimeout);
    }

    public static AutomationClientActivator getInstance() {
        return instance;
    }
}
