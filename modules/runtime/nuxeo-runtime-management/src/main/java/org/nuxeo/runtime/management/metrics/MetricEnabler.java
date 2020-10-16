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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.javasimon.Simon;
import org.javasimon.SimonManager;
import org.javasimon.jmx.JmxRegisterCallback;
import org.javasimon.utils.LoggingCallback;

// @deprecated since 11.4: use dropwizard metrics instead
@Deprecated(since = "11.4")
public class MetricEnabler implements MetricEnablerMXBean {

    protected MetricSerializer serializer;

    protected LoggingCallback lgCB;

    protected final JmxRegisterCallback jmxCB = new JmxRegisterCallback();

    protected void setSerializer(MetricSerializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public void enable() {
        SimonManager.enable();
        SimonManager.callback().addCallback(jmxCB);
        for (String name : SimonManager.simonNames()) {
            Simon simon = SimonManager.getSimon(name);
            jmxCB.simonCreated(simon);
        }
    }

    @Override
    public void disable() {
        SimonManager.callback().removeCallback(jmxCB);
        for (String name : SimonManager.simonNames()) {
            Simon simon = SimonManager.getSimon(name);
            jmxCB.simonDestroyed(simon);
        }
        SimonManager.disable();
    }

    @Override
    public boolean isEnabled() {
        return SimonManager.isEnabled();
    }

    @Override
    public void enableLogging() {
        lgCB = new LoggingCallback();
        lgCB.setLogger(Logger.getLogger("org.javasimon"));
        lgCB.setLevel(Level.FINEST);
        SimonManager.callback().addCallback(lgCB);
    }

    @Override
    public void disableLogging() {
        SimonManager.callback().removeCallback(lgCB);
        lgCB = null;
    }

    @Override
    public boolean isLogging() {
        return lgCB != null;
    }

    protected MetricSerializingCallback srzCB;

    @Override
    public void enableSerializing() throws IOException {
        serializer.resetOutput();
        srzCB = new MetricSerializingCallback(serializer);
        SimonManager.callback().addCallback(srzCB);
    }

    @Override
    public void disableSerializing() throws IOException {
        serializer.closeOutput();
        SimonManager.callback().removeCallback(srzCB);
        srzCB = null;
    }

    @Override
    public boolean isSerializing() {
        return srzCB != null;
    }

}
