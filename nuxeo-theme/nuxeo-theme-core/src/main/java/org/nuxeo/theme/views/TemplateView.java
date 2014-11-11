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

package org.nuxeo.theme.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.rendering.RenderingInfo;

public class TemplateView extends AbstractView {

    private static final Log log = LogFactory.getLog(TemplateView.class);

    @Override
    public String render(final RenderingInfo info) {
        final ViewType viewType = getViewType();
        final String template = viewType.getTemplate();
        return getTemplateContent(template);
    }

    public String getTemplateContent(final String template) {
        String result = "";
        InputStream is = null;
        try {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                    template);
            if (is == null) {
                String warTemplate = "nuxeo.war/" + template;
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        warTemplate);
            }
            if (is == null) {
                log.warn("Template file not found: " + template);
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
                    log.error(e, e);
                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
            }
        } catch (IOException e) {
            log.error(e, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.error(e, e);
                } finally {
                    is = null;
                }
            }
        }
        return result.trim();
    }
}
