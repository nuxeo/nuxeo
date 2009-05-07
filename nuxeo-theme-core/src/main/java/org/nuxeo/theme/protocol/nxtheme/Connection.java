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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementRenderer;
import org.nuxeo.theme.rendering.RenderingInfo;
import org.nuxeo.theme.themes.ThemeManager;

public final class Connection extends URLConnection {

    private static final Log log = LogFactory.getLog(Connection.class);

    private final URL url;

    private final String host;

    protected Connection(URL url) {
        super(url);
        this.url = url;
        host = url.getHost();
    }

    @Override
    public void connect() {
        connected = true;
    }

    @Override
    public long getLastModified() {
        // cache themes until they are modified
        if (host.equals("theme")) {
            return Manager.getThemeManager().getLastModified(url);
        }
        // do not cache elements
        else if (host.equals("element")) {
            return System.currentTimeMillis();
        }
        // cache everything else
        return 0L;
    }

    @Override
    public InputStream getInputStream() throws IOException {

        Element rendered = null;
        boolean cache = true;

        ThemeManager themeManager = Manager.getThemeManager();

        // render a single element
        if (host.equals("element")) {
            rendered = ThemeManager.getElementByUrl(url);
            cache = false;
        }

        // render the entire theme
        else if (host.equals("theme")) {
            rendered = themeManager.getThemeByUrl(url);
        }

        if (rendered == null) {
            // no such element found in the theme definitions: throws
            // IOException since it is the most semantically suited Exception
            // declared in the java.net.URLConnection based class
            throw new IOException(String.format("Error while rendering %s",
                    ThemeManager.getUrlDescription(url)));
        }

        final RenderingInfo info = new RenderingInfo(rendered, url);
        final String content = ElementRenderer.render(info, cache).getMarkup();
        if (host.equals("theme")) {
            if (log.isTraceEnabled()) {
                log.trace(String.format("NXThemes output for %s: \n%s\n",
                        url.toString(), content));
            }
        }
        return new ByteArrayInputStream(content.getBytes());
    }

}
