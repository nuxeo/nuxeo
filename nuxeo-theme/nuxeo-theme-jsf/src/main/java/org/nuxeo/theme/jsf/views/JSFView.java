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

package org.nuxeo.theme.jsf.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.models.InfoPool;
import org.nuxeo.theme.rendering.RenderingInfo;
import org.nuxeo.theme.resources.ResourceManager;
import org.nuxeo.theme.views.AbstractView;
import org.nuxeo.theme.views.ViewType;

public class JSFView extends AbstractView {

    private static final Log log = LogFactory.getLog(JSFView.class);

    public String render(final RenderingInfo info) {
        final  URL themeUrl = info.getThemeUrl();
        final ViewType viewType = getViewType();
        final String template = viewType.getTemplate();

        final ResourceManager resourceManager = Manager.getResourceManager();
        final InfoPool infoPool = Manager.getInfoPool();

        for (String resource : viewType.getResources()) {
            resourceManager.addResource(resource, themeUrl);
        }
        String result = "";
        InputStream is = null;

        try {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                    template);
            if (is == null) {
                log.warn("View template not found: " + template);
            } else {
                Reader in = null;
                try {
                    in = new BufferedReader(new InputStreamReader(is));
                    StringBuilder rendered = new StringBuilder();
                    int ch;
                    while ((ch = in.read()) > -1) {
                        rendered.append((char) ch);
                    }
                    result = rendered.toString();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    is = null;
                }
            }
        }

        // place data structure references inside the template
        final String infoId = infoPool.computeInfoId(info);
        result = result.replaceAll("nxthemesInfo.", String.format(
                "nxthemesInfo.map.%s.", infoId));

        // replace [nxthemes markup] strings with the actual markup
        result = result.replace("[nxthemes markup]", info.getMarkup());

        return result;
    }

}
