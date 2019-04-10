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
import org.nuxeo.wss.fm.FreeMarkerRenderer;
import org.nuxeo.wss.servlet.WSSRequest;
import org.nuxeo.wss.spi.dummy.DummyBackendFactory;

public class Backend {

    protected static WSSBackendFactory factory = null;
    public static final String DEFAULT_FACTORY_CLASS = "org.nuxeo.ecm.platform.wi.backend.wss.WSSBackendFactoryImpl";

    private static final Log log = LogFactory.getLog(Backend.class);

    public static WSSBackendFactory getFactory() {
        if (factory == null) {
            factory = loadFactory();
            if (factory != null) {
                FreeMarkerRenderer.addLoader(factory.getClass());
            }
        }
        return factory;
    }

    protected synchronized static WSSBackendFactory loadFactory() {
        String factoryClass = WSSConfig.instance().getWssBackendFactoryClassName();

        /*@TODO: uncomment this after set org.nuxeo.ecm.platform.wi.backend.wss.WSSBackendFactoryImpl
         as default factory in WSSFilter config 
        /*try {
            factory = (WSSBackendFactory) Class.forName(factoryClass, true,
                    Thread.currentThread().getContextClassLoader()).newInstance();
        } catch (Exception e) {
            log.warn("Unable to create backend factory " + factoryClass + ". Message:" + e.getMessage()
                    + ". Try load default factory " + DEFAULT_FACTORY_CLASS);
            factory = null;
        }*/

        if (factory == null) {
        try {
                factory = (WSSBackendFactory) Class.forName(DEFAULT_FACTORY_CLASS, true,
                        Thread.currentThread().getContextClassLoader()).newInstance();
        } catch (Exception e) {
                log.error("Unable to create default backend factory " + DEFAULT_FACTORY_CLASS, e);
            }
        }

        //for tests
        if(factory == null){
            factory = new DummyBackendFactory();
        }
        return factory;
    }

    public static WSSBackend get(WSSRequest request) {
        return getFactory().getBackend(request);
    }

}
