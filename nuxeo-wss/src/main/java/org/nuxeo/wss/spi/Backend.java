/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.wss.spi;

import org.nuxeo.wss.fm.FreeMarkerRenderer;
import org.nuxeo.wss.impl.WSSBackendFactoryImpl;
import org.nuxeo.wss.servlet.WSSRequest;

public class Backend {

    // set directly for tests
    public static WSSBackendFactory factory = null;

    public static WSSBackendFactory getFactory() {
        if (factory == null) {
            factory = new WSSBackendFactoryImpl();
            if (factory != null) {
                FreeMarkerRenderer.addLoader(factory.getClass());
            }
        }
        return factory;
    }

    public static WSSBackend get(WSSRequest request) {
        return getFactory().getBackend(request);
    }

}
