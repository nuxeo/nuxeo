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

package org.nuxeo.ecm.core.url.nxdoc;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.url.nxobj.ObjectURL;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PropertyURL implements URLStreamHandlerFactory {

    public static URL getURL(DocumentModel doc, String xpath) {
        return ObjectURL.getURL(doc, xpath, Handler.getInstance());
    }

    public static URL createURL(String repository, String docId, String xpath) throws MalformedURLException {
        return new URL("nxdoc",  repository, 0, docId +"/"+ xpath, Handler.getInstance());
    }

    public static URL createURL(String repository, String docId, String xpath, String sid) throws MalformedURLException {
        return new URL("nxdoc",  repository, 0, docId +"/" + xpath + "#" + sid, Handler.getInstance());
    }

    public static URL createURL(DocumentModel doc, String xpath) throws MalformedURLException {
        return createURL(doc.getRepositoryName(), doc.getId(), xpath, doc.getSessionId());
    }

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if ("nxdoc".equals(protocol)) {
            return new Handler();
        }
        return null;
    }

}
