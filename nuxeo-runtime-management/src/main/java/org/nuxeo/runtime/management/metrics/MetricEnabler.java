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
import org.javasimon.utils.LoggingCallback;

public class MetricEnabler implements MetricEnablerMBean {;

    protected MetricSerializer serializer;

    protected MetricRegister register;

    protected MetricRegisteringCallback regCb;

    protected MetricSerializingCallback srzCb;

    protected LoggingCallback lgCB;


    protected MetricEnabler(MetricSerializer serializer, MetricRegister register) {
        this.serializer = serializer;
        this.register = register;
    }

    @Override
    public void enable() {
        SimonManager.enable();
        regCb = new MetricRegisteringCallback(register);
        SimonManager.callback().addCallback(regCb);
        for (String name : SimonManager.getSimonNames()) {
            Simon simon = SimonManager.getSimon(name);
            regCb.onSimonCreated(simon);
        }
        enableLogging();
    }

    @Override
    public void disable() {
        SimonManager.callback().removeCallback(regCb);
        for (String name : SimonManager.getSimonNames()) {
            Simon simon = SimonManager.getSimon(name);
            regCb.onSimonDestroyed(simon);
        }
        regCb = null;
        SimonManager.disable();
    }

    @Override
    public boolean isEnabled() {
        return SimonManager.isEnabled();
    }

    @Override
    public void enableLogging() {
        Logger logger = Logger.getLogger("org.javasimon");
        if (!logger.isLoggable(Level.FINE)) {
            return;
        }
        lgCB = new LoggingCallback();
        lgCB.setLogger(Logger.getLogger("org.javasimon"));
        lgCB.setLevel(Level.FINEST);
        SimonManager.callback().addCallback(lgCB);
    }

    @Override
    public void disableLogging() {
        Logger logger = Logger.getLogger("org.javasimon");
        if (!logger.isLoggable(Level.FINE)) {
            return;
        }
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
