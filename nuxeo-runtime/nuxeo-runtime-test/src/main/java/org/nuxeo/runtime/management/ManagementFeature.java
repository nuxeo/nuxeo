package org.nuxeo.runtime.management;

import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.SimpleFeature;

@Deploy("org.nuxeo.runtime.management")
@LocalDeploy("org.nuxeo.runtime.management:isolated-server.xml")
@Features(RuntimeFeature.class)
public class ManagementFeature extends SimpleFeature {

}
