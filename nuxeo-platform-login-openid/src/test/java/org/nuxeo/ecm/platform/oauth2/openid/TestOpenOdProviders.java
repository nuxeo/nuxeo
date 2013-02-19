package org.nuxeo.ecm.platform.oauth2.openid;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(repositoryName = "default", type = BackendType.H2, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.directory.sql", "org.nuxeo.ecm.directory",
        "org.nuxeo.ecm.directory.api",
        "org.nuxeo.ecm.platform.login.openid.test" })
public class TestOpenOdProviders {

    @Test
    public void verifyServiceRegistration() {

        OpenIDConnectProviderRegistry registry = Framework.getLocalService(OpenIDConnectProviderRegistry.class);
        Assert.assertNotNull(registry);

        OpenIDConnectProvider provider = registry.getProvider("TestingGoogleOpenIDConnect");
        Assert.assertNotNull(provider);

        Assert.assertTrue(OpenIDConnectHelper.getEnabledProviders().size() > 0);

        OpenIDConnectProvider provider2 = registry.getProvider("TestingGoogleOpenIDConnect2");
        Assert.assertNotNull(provider2);

    }

}
