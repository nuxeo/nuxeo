/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.management.standby;

import java.time.Duration;
import java.time.Instant;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ObjectNameFactory;

public class StandbyCommand  implements StandbyMXBean {

    @Override
    public void standby(int delay) throws InterruptedException {
        if (Framework.getRuntime().isStandby()) {
            return;
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(Framework.class.getClassLoader());
        try {
            Framework.getRuntime().standby(Instant.now().plus(Duration.ofSeconds(delay)));
        } finally {
            Thread.currentThread().setContextClassLoader(loader);
        }
    }

    @Override
    public void resume() {
        if (!Framework.getRuntime().isStandby()) {
            return;
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(Framework.class.getClassLoader());
        try {
            Framework.getRuntime().resume();
        } finally {
            Thread.currentThread().setContextClassLoader(loader);
        }
    }

    @Override
    public boolean isStandby() {
        return Framework.getRuntime().isStandby();
    }

    protected final Registration registration = new Registration();

    protected class Registration {

        protected MBeanServer server;

        protected ObjectInstance instance;

        protected Registration with(MBeanServer server) {
            this.server = server;
            return this;
        }

        protected Registration register() throws JMException {
            instance = server.registerMBean(StandbyCommand.this, ObjectNameFactory.getObjectName(StandbyCommand.class.getName()));
            return this;
        }

        protected void unregister() throws JMException {
            try {
                server.unregisterMBean(instance.getObjectName());
            } finally {
                instance = null;
            }
        }
    }
}
