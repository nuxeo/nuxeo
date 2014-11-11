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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.wss.WSSConfig;
import org.nuxeo.wss.servlet.WSSRequest;

public class Backend {

    protected static WSSBackendFactory factory = null;

    private static final Log log = LogFactory.getLog(Backend.class);

    public static WSSBackendFactory getFactory() {
        if (factory == null) {
            factory = loadFactory();
        }
        return factory;
    }

    protected synchronized static WSSBackendFactory loadFactory() {
        String factoryClass = WSSConfig.instance().getWssBackendFactoryClassName();
        try {
            factory = (WSSBackendFactory) Class.forName(factoryClass, true, Thread.currentThread().getContextClassLoader()).newInstance();
        } catch (Exception e) {
            log.error("Unable to create backend factory", e);
        }
        return factory;
    }

    public static WSSBackend get(WSSRequest request) {
        return getFactory().getBackend(request);
    }

}
