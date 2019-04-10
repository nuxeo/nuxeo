package org.nuxeo.dmk.test;

import java.lang.management.ManagementFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.dmk-adaptor")
public class TestDMKAdaptor {

	@Test public void shouldDeploy() throws InstanceNotFoundException, MalformedObjectNameException {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectInstance htmlAdaptor = mbs.getObjectInstance(new ObjectName("org.nuxeo:type=jmx-adaptor,format=html"));
        Assert.assertThat(htmlAdaptor, org.hamcrest.Matchers.notNullValue());
        ObjectInstance httpConnector = mbs.getObjectInstance(new ObjectName("org.nuxeo:type=jmx-connector,protocol=jdmk-http"));
        Assert.assertThat(httpConnector, org.hamcrest.Matchers.notNullValue());
        ObjectInstance httpsConnector = mbs.getObjectInstance(new ObjectName("org.nuxeo:type=jmx-connector,protocol=jdmk-https"));
        Assert.assertThat(httpsConnector, org.hamcrest.Matchers.notNullValue());
	}

}
