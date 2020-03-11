/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.runtime.management.metrics;

import org.javasimon.SimonManager;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.osgi.framework.BundleContext;

public class MetricComponent extends DefaultComponent {

    protected final MetricSerializer serializer = new MetricSerializer();

    protected final MetricEnabler enabler = new MetricEnabler();

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (MetricSerializerMXBean.class.isAssignableFrom(adapter)) {
            return adapter.cast(serializer);
        }
        if (MetricEnabler.class.isAssignableFrom(adapter)) {
            return adapter.cast(enabler);
        }
        if (MetricSerializer.class.isAssignableFrom(adapter)) {
            return adapter.cast(serializer);
        }
        return super.getAdapter(adapter);
    }

    protected final MetricRegister register = new MetricRegister();

    protected final MetricRegisteringCallback registeringCB = new MetricRegisteringCallback(register);

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        doStart();
    }

    @Override
    public void deactivate(ComponentContext context) {
        doStop();
        super.deactivate(context);
    }

    public void start(BundleContext context) {
        doStart();
    }

    public void stop(BundleContext context) {
        doStop();
    }

    protected void doStart() {
        enabler.setSerializer(serializer);
        SimonManager.enable();
        SimonManager.callback().addCallback(registeringCB);
        register.registerMXBean(enabler, "enabler", MetricEnablerMXBean.class, "Feature");
        register.registerMXBean(serializer, "serializer", MetricSerializerMXBean.class, "Feature");
    }

    protected void doStop() {
        SimonManager.disable();
        if (SimonManager.callback() != null) {
            SimonManager.callback().removeCallback(registeringCB);
        }
        register.unregisterAll();
    }

}
