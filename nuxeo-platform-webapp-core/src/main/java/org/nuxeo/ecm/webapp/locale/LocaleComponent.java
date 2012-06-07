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
 *     matic
 */
package org.nuxeo.ecm.webapp.locale;

import java.util.Locale;
import java.util.TimeZone;

import org.jboss.seam.Component;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webapp.delegate.DocumentManagerBusinessDelegate;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author matic
 *
 */
public class LocaleComponent extends DefaultComponent {

    protected LocaleProvider provider;

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if ("providers".equals(extensionPoint)) {
            provider = ((LocaleProviderDescriptor)contribution).newProvider();
        }
    }

    public LocaleProvider getProvider() {
        return provider;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (provider == null) {
            return super.getAdapter(adapter);
        }
        CoreSession repo = (CoreSession)Component.getInstance(DocumentManagerBusinessDelegate.class);
        if (TimeZone.class.equals(adapter)) {
            try {
                return adapter.cast(provider.getTimeZone(repo));
            } catch (ClientException e) {
                throw new RuntimeException("Cannot get time zone", e);
            }
        } else if (Locale.class.equals(adapter)) {
            try {
                return adapter.cast(provider.getLocale(repo));
            } catch (ClientException e) {
                throw new RuntimeException("Cannot get locale", e);
            }
        }
        return super.getAdapter(adapter);
    }

}
