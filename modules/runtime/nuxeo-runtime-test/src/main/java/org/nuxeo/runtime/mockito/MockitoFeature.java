/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nuxeo.runtime.mockito;

import static org.mockito.MockitoAnnotations.initMocks;

import java.lang.reflect.Field;

import org.mockito.configuration.IMockitoConfiguration;
import org.mockito.internal.configuration.GlobalConfiguration;
import org.nuxeo.runtime.api.DefaultServiceProvider;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

public class MockitoFeature implements RunnerFeature {

    protected final MockProvider provider = new MockProvider();

    @Override
    public void start(FeaturesRunner runner) {
        provider.installSelf();
    }

    @Override
    public void testCreated(Object test) {
        DefaultServiceProvider.setProvider(provider);
        initMocks(test);
    }

    @Override
    public void afterRun(FeaturesRunner runner) throws Exception {
        cleanupThread();
    }

    @Override
    public void stop(FeaturesRunner runner) {
        provider.uninstallSelf();
    }

    protected void cleanupThread() throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Field f = GlobalConfiguration.class.getDeclaredField("globalConfiguration");
        f.setAccessible(true);
        ThreadLocal<IMockitoConfiguration> holder = (ThreadLocal<IMockitoConfiguration>) f.get(null);
        holder.remove();
    }
}
