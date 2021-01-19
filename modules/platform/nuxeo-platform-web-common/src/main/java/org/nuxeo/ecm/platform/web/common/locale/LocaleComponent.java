/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 *      Stephane Lacoin at Nuxeo (aka matic) <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.platform.web.common.locale;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Allow external components to provide locale and timezone to be used in the application.
 *
 * @since 5.6
 */
public class LocaleComponent extends DefaultComponent {

    protected static final String XP = "providers";

    protected LocaleProvider provider;

    @Override
    public void start(ComponentContext context) {
        provider = this.<LocaleProviderDescriptor> getRegistryContribution(XP)
                       .map(LocaleProviderDescriptor::newProvider)
                       .orElse(null);
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        provider = null;
    }

    public LocaleProvider getProvider() {
        return provider;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (LocaleProvider.class.equals(adapter)) {
            return adapter.cast(provider);
        }
        return super.getAdapter(adapter);
    }

}
