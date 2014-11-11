/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.url;


import java.net.MalformedURLException;
import java.net.URL;

import org.nuxeo.ecm.core.url.nxdoc.Handler;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class URLFactory {

    private URLFactory() {
    }

    public static URL getURL(String url) throws MalformedURLException {
        if (url.startsWith("nxdoc:")) {
            return new URL(null, url, Handler.getInstance());
        } else if (url.startsWith("nxobj:")) {
            return new URL(null, url, Handler.getInstance());
        } else {
            return new URL(url);
        }
    }

}
