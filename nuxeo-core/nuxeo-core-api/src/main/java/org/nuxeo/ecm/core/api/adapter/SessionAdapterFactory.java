/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.adapter;

import java.util.Hashtable;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class SessionAdapterFactory<T> {

    public abstract T getAdapter(CoreSession session);

    static final Map<String, SessionAdapterFactory<?>> adapters = new Hashtable<String, SessionAdapterFactory<?>>();

    public static void registerAdapter(String itf, SessionAdapterFactory<?> adapter) {
        adapters.put(itf, adapter);
    }

    public static void unregisterAdapter(String itf) {
        adapters.remove(itf);
    }

    public static SessionAdapterFactory<?> getAdapter(String itf) {
        return adapters.get(itf);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getAdapter(CoreSession session, Class<T> itf) {
        SessionAdapterFactory<T> factory = (SessionAdapterFactory<T>) getAdapter(itf.getName());
        if (factory != null) {
            return factory.getAdapter(session);
        }
        return null;
    }

}
