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

package org.nuxeo.theme.html.ui;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.Utils;
import org.nuxeo.theme.resources.ResourceType;
import org.nuxeo.theme.types.TypeFamily;

public class MVCElement {

    private static final Log log = LogFactory.getLog(MVCElement.class);

    public static String render(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();

        String resource = params.get("resource");
        String url = params.get("url");
        String body = params.get("body");
        String className = params.get("className");

        sb.append(String.format("<ins class=\"%s\">", className));

        /* insert the content from a file source */
        if (null != resource) {
            ResourceType resourceType = (ResourceType) Manager.getTypeRegistry().lookup(
                    TypeFamily.RESOURCE, resource);
            if (resourceType == null) {
                log.warn("Could not find resource: " + resource);
            } else {
                try {
                    sb.append(Utils.readResourceAsString(resourceType.getPath()));
                } catch (IOException e) {
                    log.warn("Could not find resource: " + resource);
                }
            }
        }

        /* get the content from a url */
        if (null != url) {
            sb.append(String.format(" cite=%s", url));
        }

        /* get the content from the body */
        if (null != body) {
            sb.append(body);
        }

        sb.append("</ins>");

        return sb.toString();
    }

}
