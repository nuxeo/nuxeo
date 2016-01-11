package org.nuxeo.ecm.collections.core.test;

import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.SimpleFeature;

@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.userworkspace.core", "org.nuxeo.ecm.platform.collections.core",
        "org.nuxeo.ecm.platform.userworkspace.types", "org.nuxeo.ecm.platform.query.api",
        "org.nuxeo.ecm.platform.web.common" })
public class CollectionFeature extends SimpleFeature {

}
