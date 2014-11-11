/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
        return super.getAdapter(adapter);
    }

    protected final MetricRegisterer register = new MetricRegisterer();

    protected final MetricRegisteringCallback registeringCB = new  MetricRegisteringCallback();

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        doStart();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
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
        register.registerMXBean(enabler);
        register.registerMXBean(serializer);
    }

    protected void doStop() {
        SimonManager.disable();
        SimonManager.callback().removeCallback(registeringCB);
        register.unregisterMXBean(enabler);
        register.unregisterMXBean(serializer);
    }

}
