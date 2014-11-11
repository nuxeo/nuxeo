/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.protocol.nxtheme;

import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public final class Handler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL url) {
        return new Connection(url);
    }

    /**
     * Theme URL do not reference any networked resource. This method is called
     * by URL.equals and URL.hashCode so that we need override it to avoid DNS
     * lookup for the theme host.
     * 
     * @param u a URL object
     * @return null for url with nxtheme protocol,
     *         URLStreamHandler.getHostAddress(u) otherwise
     */
    @Override
    protected synchronized InetAddress getHostAddress(URL u) {
        if ("nxtheme".equals(u.getProtocol())) {
            // do not do a DNS lookup for a theme resource
            return null;
        }
        return super.getHostAddress(u);
    }

}
