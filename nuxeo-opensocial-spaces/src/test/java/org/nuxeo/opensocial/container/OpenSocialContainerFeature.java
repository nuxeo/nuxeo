package org.nuxeo.opensocial.container;

import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.SimpleFeature;

@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.picture.core",
        "org.nuxeo.ecm.platform.htmlsanitizer",
        "org.nuxeo.ecm.opensocial.spaces" })
public class OpenSocialContainerFeature extends SimpleFeature {

}
