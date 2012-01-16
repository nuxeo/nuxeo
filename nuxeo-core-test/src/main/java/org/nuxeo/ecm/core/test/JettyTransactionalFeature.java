package org.nuxeo.ecm.core.test;

import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.JettyFeature;
import org.nuxeo.runtime.test.runner.SimpleFeature;

@Features({ JettyFeature.class })
@Deploy({ "org.nuxeo.runtime.jtajca", "org.nuxeo.ecm.core.test:OSGI-INF/jetty-transactional-contrib.xml" })
public class JettyTransactionalFeature extends SimpleFeature  {

}
