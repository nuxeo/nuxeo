package org.nuxeo.runtime.mockito;

import static org.mockito.MockitoAnnotations.initMocks;

import java.lang.reflect.Field;

import org.mockito.configuration.IMockitoConfiguration;
import org.mockito.internal.configuration.GlobalConfiguration;
import org.nuxeo.runtime.api.DefaultServiceProvider;
import org.nuxeo.runtime.test.protocols.inline.InlineURLFactory;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.SimpleFeature;

@Deploy("org.mockito.mockito-all")
@Features({RuntimeFeature.class})
public class MockitoFeature extends SimpleFeature {

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        InlineURLFactory.install();
    }

    @Override
    public void testCreated(Object test) throws Exception {
        DefaultServiceProvider.setProvider(MockProvider.INSTANCE);
        initMocks(test);
    }

    @Override
    public void afterRun(FeaturesRunner runner) throws Exception {
       cleanupThread();
    }

    @Override
    public void stop(FeaturesRunner runner) throws Exception {
        InlineURLFactory.uninstall();
    }

    protected void cleanupThread() throws NoSuchFieldException,
            SecurityException, IllegalArgumentException, IllegalAccessException {
        Field f = GlobalConfiguration.class.getDeclaredField("globalConfiguration");
           f.setAccessible(true);
           ThreadLocal<IMockitoConfiguration> holder = (ThreadLocal<IMockitoConfiguration>) f.get(null);
           holder.remove();
    }
}
