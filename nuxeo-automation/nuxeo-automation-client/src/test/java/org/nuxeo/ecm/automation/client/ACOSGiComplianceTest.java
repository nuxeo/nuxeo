/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * @since 5.7 Automation Client OSGi compliance test
 */
@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ACOSGiComplianceTest {

    @Inject
    private BundleContext bc;

    @Configuration
    public Option[] config() {

        return options(
                vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),
                mavenBundle("org.nuxeo.ecm.automation",
                        "nuxeo-automation-client", "5.7-SNAPSHOT"),
                junitBundles());
    }

    @Test
    public void getHelloService() {
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
        // Check services from automation client
        ServiceReference[] serviceReferences = acBundle.getRegisteredServices();
    }
}
