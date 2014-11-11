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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.javasimon.Simon;
import org.javasimon.SimonManager;
import org.javasimon.jmx.JmxRegisterCallback;
import org.javasimon.utils.LoggingCallback;

public class MetricEnabler implements MetricEnablerMXBean {;

    protected MetricSerializer serializer;

    protected LoggingCallback lgCB;

    protected final JmxRegisterCallback jmxCB = new JmxRegisterCallback();

    protected void setSerializer(MetricSerializer serializer) {
        this.serializer = serializer;
    }

    public void enable() {
        SimonManager.enable();
        SimonManager.callback().addCallback(jmxCB);
        for (String name : SimonManager.simonNames()) {
            Simon simon = SimonManager.getSimon(name);
            jmxCB.simonCreated(simon);
        }
    }

    public void disable() {
        SimonManager.callback().removeCallback(jmxCB);
        for (String name : SimonManager.simonNames()) {
            Simon simon = SimonManager.getSimon(name);
            jmxCB.simonDestroyed(simon);
        }
        SimonManager.disable();
    }

    public boolean isEnabled() {
        return SimonManager.isEnabled();
    }

    public void enableLogging() {
        lgCB = new LoggingCallback();
        lgCB.setLogger(Logger.getLogger("org.javasimon"));
        lgCB.setLevel(Level.FINEST);
        SimonManager.callback().addCallback(lgCB);
    }

    public void disableLogging() {
        SimonManager.callback().removeCallback(lgCB);
        lgCB = null;
    }

    public boolean isLogging() {
        return lgCB != null;
    }

    protected MetricSerializingCallback srzCB;

    public void enableSerializing() throws IOException {
            serializer.resetOutput();
            srzCB = new MetricSerializingCallback(serializer);
            SimonManager.callback().addCallback(srzCB);
    }

    public void disableSerializing() throws IOException {
            serializer.closeOutput();
            SimonManager.callback().removeCallback(srzCB);
            srzCB = null;
    }

    public boolean isSerializing() {
        return srzCB != null;
    }

}
