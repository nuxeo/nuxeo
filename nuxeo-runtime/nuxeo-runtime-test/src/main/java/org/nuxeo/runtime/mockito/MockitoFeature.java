/*******************************************************************************
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *******************************************************************************/
package org.nuxeo.runtime.mockito;

import static org.mockito.MockitoAnnotations.initMocks;

import java.lang.reflect.Field;

import org.mockito.configuration.IMockitoConfiguration;
import org.mockito.internal.configuration.GlobalConfiguration;
import org.nuxeo.runtime.api.DefaultServiceProvider;
import org.nuxeo.runtime.test.protocols.inline.InlineURLFactory;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;

public class MockitoFeature extends SimpleFeature {


    protected final MockProvider provider = new MockProvider();

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        InlineURLFactory.install();
        provider.installSelf();
    }

    @Override
    public void testCreated(Object test) throws Exception {
        DefaultServiceProvider.setProvider(provider);
        initMocks(test);
    }

    @Override
    public void afterRun(FeaturesRunner runner) throws Exception {
       cleanupThread();
    }

    @Override
    public void stop(FeaturesRunner runner) throws Exception {
        InlineURLFactory.uninstall();
        provider.uninstallSelf();
    }

    protected void cleanupThread() throws NoSuchFieldException,
            SecurityException, IllegalArgumentException, IllegalAccessException {
        Field f = GlobalConfiguration.class.getDeclaredField("globalConfiguration");
           f.setAccessible(true);
           ThreadLocal<IMockitoConfiguration> holder = (ThreadLocal<IMockitoConfiguration>) f.get(null);
           holder.remove();
    }
}
