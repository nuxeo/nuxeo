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
package org.nuxeo.wss;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages cache for Call Handlers
 *
 * @author Thierry Delprat
 */
public class CallRouter {

    protected static Map<String, Object> handlers = new HashMap<String, Object>();

    protected static <T> T getHandler(Class<T> handlerClass, String handlerName) throws WSSException {

        Object handler = handlers.get(handlerName);

        if (handler == null) {
            Class klass;
            try {
                String pkg_prefix = handlerClass.getPackage().getName();
                klass = Class.forName(pkg_prefix + "." + handlerName);
                handler = klass.newInstance();
                handlers.put(handlerName, handler);
            } catch (ReflectiveOperationException e) {
                throw new WSSException("Unable to find handler", e);

            }
        }
        return handlerClass.cast(handler);
    }

}
