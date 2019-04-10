/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thierry Delprat
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

    @Override
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
        StringBuilder links = new StringBuilder();
        StringBuilder relativePath = new StringBuilder();

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

    protected String buildLink(String basePath, StringBuilder relativePath, String str) {
        return String.format(LINK_TEMPLATE, basePath + relativePath, str);
    }

}
