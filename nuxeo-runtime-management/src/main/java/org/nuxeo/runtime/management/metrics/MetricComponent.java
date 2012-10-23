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

import org.javasimon.Simon;
import org.javasimon.SimonManager;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;


public class MetricComponent extends DefaultComponent implements MetricAttributesProvider, MetricHistoryProvider {

    protected final MetricSerializer serializer = new MetricSerializer();

    protected final MetricRegister register = new MetricRegister();

    protected final MetricEnabler enabler = new MetricEnabler(serializer, register);

    protected final MetricHistoryRecorder history = new MetricHistoryRecorder(50);


    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (MetricSerializer.class.isAssignableFrom(adapter)) {
            return adapter.cast(serializer);
        }
        if (MetricEnabler.class.isAssignableFrom(adapter)) {
            return adapter.cast(enabler);
        }
        if (MetricRegister.class.isAssignableFrom(adapter)) {
            return adapter.cast(register);
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        doStart();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        doStop();
        super.deactivate(context);
    }


    protected void doStart() {
        enabler.enable();
        register.registerMXBean(enabler,  "enabler", MetricEnabler.class, "Feature");
        register.registerMXBean(serializer, "serializer", MetricSerializer.class, "Feature");
        SimonManager.manager().callback().addCallback(history);
    }

    protected void doStop() {
        enabler.disable();
        register.unregisterMXBean("enabler");
        register.unregisterMXBean("serializer");
        SimonManager.manager().callback().removeCallback(history);
        history.clearStacks();
    }


    @Override
    public MetricHistoryStack getStack(String name) {
        return history.getStack(name);
    }


    @Override
    public MetricAttributes getAttributes(String name) {
        Simon simon = SimonManager.getSimon(name);
        return new SimonAttributes(simon);
    }


}
