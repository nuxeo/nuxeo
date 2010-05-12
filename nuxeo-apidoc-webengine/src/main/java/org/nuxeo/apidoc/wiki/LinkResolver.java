/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.apidoc.wiki;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.ecm.platform.rendering.wiki.WikiFilter;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebContext;

public class LinkResolver implements WikiFilter {

    public static final String PATTERN = "(\\.)?([A-Z]+[a-z]+[A-Z][A-Za-z]*\\.)*([A-Z]+[a-z]+[A-Z][A-Za-z]*)";
    public static final Pattern LINK_PATTERN = Pattern.compile(PATTERN);
    public static final String LINK_TEMPLATE = "<a href=\"%s\">%s</a>";

    public String apply(String content) {
        Matcher m = LINK_PATTERN.matcher(content);
        StringBuffer sb = new StringBuffer();
        if (!m.find()) {
            return content;
        }
        do {
            String s = m.group();
            String link = buildLinks(s);
            m.appendReplacement(sb, link);
        } while (m.find());
        m.appendTail(sb);
        return sb.toString();
    }

    protected String buildLinks(String pageName) {
        String basePath;

        WebContext ctx = WebEngine.getActiveContext();
        Resource resource = ctx.getTargetObject();
        StringBuffer links = new StringBuffer();
        StringBuffer relativePath = new StringBuffer();

        if (pageName.startsWith(".")) {
            // Absolute path
            basePath = ctx.getModulePath();
            String[] segments = pageName.substring(1).split("\\.");

            Resource parentResource = resource.getPrevious();
            while (!parentResource.isInstanceOf("site")) {
                parentResource = parentResource.getPrevious();
            }
            relativePath.append("/").append(parentResource.getName());
            for (String segment : segments) {
                links.append(".");
                relativePath.append("/").append(segment);
                links.append(buildLink(basePath, relativePath, segment));
            }
        } else {
            // Relative path
            basePath = resource.getPath();
            String[] segments = pageName.split("\\.");
            for (String segment : segments) {
                relativePath.append("/").append(segment);
                links.append(buildLink(basePath, relativePath, segment));
                links.append(".");
            }
            // Remove last dot
            links.deleteCharAt(links.length() - 1);
        }

        return links.toString();
    }

    protected String buildLink(String basePath, StringBuffer relativePath,
            String str) {
        return String.format(LINK_TEMPLATE, basePath + relativePath, str);
    }

}
