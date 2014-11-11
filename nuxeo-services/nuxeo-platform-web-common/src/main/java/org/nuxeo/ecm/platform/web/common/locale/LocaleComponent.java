/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *      Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 *      Stephane Lacoin at Nuxeo (aka matic) <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.platform.web.common.locale;

import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Allow external components to provide locale and timezone to be used in the
 * application.
 *
 * @since 5.6
 */
public class LocaleComponent extends DefaultComponent {

    protected LocaleProvider provider;

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if ("providers".equals(extensionPoint)) {
            provider = ((LocaleProviderDescriptor) contribution).newProvider();
        }
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
