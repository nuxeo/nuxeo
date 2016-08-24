/*
 * (C) Copyright 2014-2017 Nuxeo (http://nuxeo.com/) and others.
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

public class LoginConfiguration extends Configuration {

    public static final LoginConfiguration INSTANCE = new LoginConfiguration();

    protected Configuration parent = null;

    protected Provider provider = null;

    protected final AtomicInteger counter = new AtomicInteger(0);

    public interface Provider {

        AppConfigurationEntry[] getAppConfigurationEntry(String name);

    }

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        AppConfigurationEntry[] entries = provider != null ? provider.getAppConfigurationEntry(name) : null;
        if (entries == null) {
            entries = parent != null ? parent.getAppConfigurationEntry(name) : null;
        }
        return entries;
    }

    @Override
    public void refresh() {
        if (parent != null) {
            parent.refresh();
        }
    }

    public void install(Provider provider) {
        int count = counter.incrementAndGet();
        if (count == 1) {
            this.provider = provider;
            this.parent = Configuration.getConfiguration();
            Configuration.setConfiguration(this);
        }
    }

    public void uninstall() {
        int count = counter.decrementAndGet();
        if (count == 0) {
            Configuration.setConfiguration(parent);
            this.provider = null;
            this.parent = null;
        }
    }

}
