package org.nuxeo.ecm.platform.oauth.tests;

import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.SimpleFeature;

@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.forms.layout.api",
        "org.nuxeo.ecm.platform.forms.layout.core",
        "org.nuxeo.ecm.platform.forms.layout.client",
        "org.nuxeo.ecm.platform.web.common",
        "org.nuxeo.ecm.platform.oauth" })
@LocalDeploy("org.nuxeo.ecm.platform.oauth:OSGI-INF/directory-test-config.xml")
public class OAuthFeature extends SimpleFeature {

}
