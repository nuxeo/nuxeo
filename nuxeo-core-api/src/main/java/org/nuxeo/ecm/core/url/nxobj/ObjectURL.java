/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.url.nxobj;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.nuxeo.ecm.core.url.nxdoc.Handler;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
// TODO use Weak references
public class ObjectURL  implements URLStreamHandlerFactory {

    static final ConcurrentMap<String, Object> objects = new ConcurrentHashMap<String, Object>();

    public static URL getURL(Object object) {
        return getURL(object, "", Handler.getInstance());
    }

    public static URL getURL(Object object, String path, Handler handler) {
        String key = Long.toHexString(System.identityHashCode(object));
        try {
            return new URL("nxobj", key, 0, path, Handler.getInstance());
        } catch (MalformedURLException e) {
            throw new RuntimeException("This cannot happen", e);
        }
    }

    public static void removeURL(URL url) {
        objects.remove(url.getHost());
    }

    public static Object getObject(URL url) {
        return objects.get(url.getHost());
    }

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if ("nxobj".equals(protocol)) {
            return new Handler();
        }
        return null;
    }

}
