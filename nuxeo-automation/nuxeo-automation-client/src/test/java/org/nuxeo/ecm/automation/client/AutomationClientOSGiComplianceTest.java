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
package org.nuxeo.ecm.automation.client;

import static org.ops4j.pax.exam.CoreOptions.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.client.jaxrs.spi.RequestInterceptor;
import org.nuxeo.ecm.automation.client.jaxrs.spi.auth.BasicAuthInterceptor;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * @since 5.7 Automation Client OSGi compliance test
 */
@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class AutomationClientOSGiComplianceTest {

    @Inject
    private BundleContext bc;

    @Configuration
    public Option[] config() throws FileNotFoundException {
        return options(baseBundle(), junitBundles(),
                bundle("reference:file:target/classes"));
    }

    // Inject all automation client dependencies
    public DefaultCompositeOption baseBundle() throws FileNotFoundException {
        DefaultCompositeOption options = new DefaultCompositeOption();
        File dependenciesDir = new File("target/dependency");
        for (File dependency : dependenciesDir.listFiles()) {
            options.add(streamBundle(new FileInputStream(dependency)));
        }
        return options;
    }

    @Test
    public void checkAutomationClientActive() throws MalformedURLException,
            URISyntaxException {
        // Check if automation client bundle is loaded
        List<Bundle> bundleList = Arrays.asList(bc.getBundles());
        Bundle acBundle = null;
        for (Bundle bundle : bundleList) {
            if ("org.nuxeo.ecm.automation.client".equals(bundle.getSymbolicName())) {
                acBundle = bundle;
                break;
            }
        }
        Assert.assertNotNull(acBundle);
        // Check if automation client bundle is active
        Assert.assertTrue(org.osgi.framework.Bundle.ACTIVE == acBundle.getState());
        // Check if automation client service is available (as
        // HttpAutomationClient is an implementation, should be available as a
        // service)
        ServiceReference ref = bc.getServiceReference(AutomationClientFactory.class.getName());
        AutomationClientFactory factory = (AutomationClientFactory) bc.getService(ref);
        Assert.assertNotNull(factory);
        AutomationClient client = factory.getClient(new URL(
                "http://localhost:8080/nuxeo/site/automation"));
        Assert.assertNotNull(client);
        // Check if client constructor with timeout is available
        client = factory.getClient(new URL(
                "http://localhost:8080/nuxeo/site/automation"), 3600);
        Assert.assertNotNull(client);
        // Check if registerPojoMarshaller feature is accessible
        client = factory.getClient(new URL(
                "http://localhost:8080/nuxeo/site/automation"));
        client.registerPojoMarshaller(acBundle.getClass());

        // Reset
        client.shutdown();
        // Test request interceptors
        client = factory.getClient(new URL(
                "http://localhost:8080/nuxeo/site/automation"));
        RequestInterceptor requestInterceptor = new BasicAuthInterceptor
                ("Administrator", "Administrator");
        client.setRequestInterceptor(requestInterceptor);
        Session session = client.getSession();
        Assert.assertNotNull(session);
    }
}
