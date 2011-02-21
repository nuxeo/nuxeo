package org.nuxeo.ecm.spaces.core;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.spaces.api.SpaceProvider;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.opensocial.spaces",
        "org.nuxeo.ecm.platform.picture.core",
        "org.nuxeo.ecm.opensocial.spaces.test:OSGI-INF/test1-spaces-contrib.xml" })
public class SpaceProviderTest {

    @Inject
    SpaceManager sm;

    @Test
    public void spaceProvidersCanBeRetrieved() throws Exception {
        Collection<SpaceProvider> providers = sm.getSpaceProviders();
        assertThat(providers.size(), is(2));
    }

    @Test
    public void spaceProviderCanBeRetrievedByName() throws Exception {
        SpaceProvider provider = sm.getSpaceProvider("homeSpace");
        assertThat(provider, is (notNullValue()));
    }

    @Test(expected=SpaceException.class)
    public void exceptionShouldBeThrownWhenProviderNotFound() throws Exception {
        sm.getSpaceProvider("spaceThatDoesNotExists");
    }

}
