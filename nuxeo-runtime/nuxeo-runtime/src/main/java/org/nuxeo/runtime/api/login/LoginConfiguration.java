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
package org.nuxeo.runtime.api.login;

import java.util.concurrent.atomic.AtomicInteger;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import org.apache.commons.logging.LogFactory;

public class LoginConfiguration extends Configuration {

    public static final LoginConfiguration INSTANCE = new LoginConfiguration();

    protected final AtomicInteger counter = new AtomicInteger(0);

    public interface Provider {

        public AppConfigurationEntry[] getAppConfigurationEntry(String name);

    }

    protected final InheritableThreadLocal<Provider> holder = new InheritableThreadLocal<Provider>() {
        @Override
        protected Provider initialValue() {
            return context.provider;
        };
    };

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        AppConfigurationEntry[] appConfigurationEntry = holder.get().getAppConfigurationEntry(name);
        if (appConfigurationEntry == null) {
            appConfigurationEntry = context.parent.getAppConfigurationEntry(name);
        }
        return appConfigurationEntry;

    }

    @Override
    public void refresh() {
        context.parent.refresh();
    }

    protected class InstallContext {
        protected final Configuration parent = Configuration.getConfiguration();

        protected final Provider provider;

        protected final Thread thread = Thread.currentThread();

        protected final Throwable stacktrace = new Throwable();

        protected InstallContext(Provider provider) {
            this.provider = provider;
        }

        @Override
        public String toString() {
            return "Login Installation Context [parent=" + parent + ", thread=" + thread + "]";
        }

    }

    protected InstallContext context;

    public void install(Provider provider) {
        holder.set(provider);
        int count = counter.incrementAndGet();
        if (count == 1) {
            context = new InstallContext(provider);
            Configuration.setConfiguration(this);
            LogFactory.getLog(LoginConfiguration.class).trace("installed login configuration", context.stacktrace);
        }
    }

    public void uninstall() {
        holder.remove();
        int count = counter.decrementAndGet();
        if (count == 0) {
            LogFactory.getLog(LoginConfiguration.class).trace("uninstalled login configuration " + context.thread,
                    context.stacktrace);
            Configuration.setConfiguration(context.parent);
            context = null;
        }
    }

    /**
     * @since 5.9.5
     */
    public void cleanupThisThread() {
        holder.remove();
    }

}
