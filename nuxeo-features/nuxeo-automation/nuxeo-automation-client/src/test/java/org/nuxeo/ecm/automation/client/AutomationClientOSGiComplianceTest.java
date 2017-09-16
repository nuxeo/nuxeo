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
package org.nuxeo.ecm.automation.client;

import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;

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

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * @since 5.7 Automation Client OSGi compliance test
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
@Ignore("OSGi compliance not necessary")
public class AutomationClientOSGiComplianceTest {

    @Inject
    private BundleContext bc;

    @Configuration
    public Option[] config() throws FileNotFoundException {
        return options(baseBundle(), junitBundles(), bundle("reference:file:target/classes"));
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
    public void checkAutomationClientActive() throws MalformedURLException, URISyntaxException {
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
        AutomationClient client = factory.getClient(new URL("http://localhost:8080/nuxeo/site/automation"));
        Assert.assertNotNull(client);
        // Check if client constructor with timeout is available
        client = factory.getClient(new URL("http://localhost:8080/nuxeo/site/automation"), 3600);
        Assert.assertNotNull(client);
        // Check if registerPojoMarshaller feature is accessible
        client = factory.getClient(new URL("http://localhost:8080/nuxeo/site/automation"));
        client.registerPojoMarshaller(acBundle.getClass());
    }
}
