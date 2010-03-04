package org.nuxeo.opensocial.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.shindig.auth.DefaultSecurityTokenDecoder;
import org.apache.shindig.auth.SecurityTokenDecoder;
import org.apache.shindig.config.ContainerConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Scopes;


@Deploy("org.nuxeo.opensocial.service")
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class SecurityTokenDecoderTest {

    public SecurityTokenDecoderTest() throws Exception {
        Injector injector = Guice.createInjector(new Module() {
            public void configure(Binder binder) {
                binder.bind(ContainerConfig.class).to(FakeContainerConfig.class).in(Scopes.SINGLETON);
            }

        });

        OpenSocialService os = Framework.getService(OpenSocialService.class);
        os.setInjector(injector);
    }


    @Test
    public void iCanGetTheSecurityTokenDecoder() throws Exception {
        SecurityTokenDecoder tokenDecoder = Framework.getService(SecurityTokenDecoder.class);
        assertNotNull(tokenDecoder);
        assertEquals(DefaultSecurityTokenDecoder.class, tokenDecoder.getClass());
    }

}
