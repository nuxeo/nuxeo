package org.nuxeo.ecm.core.test;

import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.JettyFeature;
import org.nuxeo.runtime.test.runner.SimpleFeature;

@Features({ TransactionalFeature.class, JettyFeature.class })
public class JettyTransactionalFeature extends SimpleFeature  {

}
