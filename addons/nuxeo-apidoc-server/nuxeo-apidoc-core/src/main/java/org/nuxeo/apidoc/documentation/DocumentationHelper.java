/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.apidoc.documentation;

import java.util.LinkedList;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.platform.htmlsanitizer.HtmlSanitizerService;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper to generate HTML for documentation strings.
 */
public class DocumentationHelper {

    private static final String BR = "<br/>";

    private static final String BR2 = "<br />";

    private static final String BR3 = "<br>";

    private static final String P = "<p/>";

    private static final String P2 = "<p />";

    // private static final String CODE_START = "<div class=\"code\"><pre>";
    private static final String CODE_START = "<code><pre>";

    // private static final String CODE_END = "</pre></div>";
    private static final String CODE_END = "</pre></code>";

    private static final String AUTHOR = "@author";

    // utility class
    private DocumentationHelper() {
    }

    /**
     * Transforms Nuxeo extension point {@code <documentation>} content into
     * HTML.
     * <p>
     * <ul>
     * <li>standalone newlines are turned into {@code <br/>}</li>
     * <li>{@code <code>} blocks are turned into a {@code <div class="code">}
     * with a {@code <pre>}</li>
     * <li>{@code @author} blocks are removed</li>
     * </ul>
     */
    public static String getHtml(String doc) {
        if (doc == null) {
            return "";
        }
        HtmlSanitizerService sanitizer;
        try {
            sanitizer = Framework.getService(HtmlSanitizerService.class);
        } catch (Exception e) {
            sanitizer = null;
        }
        if (sanitizer == null && !Framework.isTestModeSet()) {
            throw new RuntimeException("Cannot find HtmlSanitizerService");
        }

        LinkedList<String> lines = new LinkedList<String>();
        lines.add(P);
        boolean newline = true;
        boolean firstcode = false;
        boolean code = false;
        for (String line : doc.split("\n")) {
            if (!code) {
                line = line.trim();
                if ("".equals(line) || BR.equals(line) || BR2.equals(line)
                        || BR3.equals(line) || P.equals(line)
                        || P2.equals(line)) {
                    if (!newline) {
                        lines.add(P);
                        newline = true;
                    }
                } else {
                    if ("<code>".equals(line)) {
                        code = true;
                        firstcode = true;
                        line = CODE_START;
                        if (!newline) {
                            line = P + line;
                        }
                        lines.add(line);
                        newline = false;
                    } else if (line.startsWith(AUTHOR)) {
                        if (!newline) {
                            lines.add(P);
                        }
                        newline = true;
                    } else {
                        lines.add(line);
                        newline = false;
                    }
                }
            } else { // code
                if ("</code>".equals(line.trim())) {
                    code = false;
                    line = CODE_END + P;
                    newline = true;
                } else {
                    line = line.replace("&", "&amp;").replace("<", "&lt;");
                }
                if (firstcode) {
                    // don't add a \n at the start of the code
                    firstcode = false;
                    line = lines.removeLast() + line;
                }
                lines.add(line);
            }
        }
        if (code) {
            lines.add(CODE_END);
        }
        String html = StringUtils.join(lines, "\n");
        if (sanitizer != null) {
            html = sanitizer.sanitizeString(html, null);
        }
        return secureXML(html);
    }

    /**
     * Makes sure no passwords are embedded in the XML.
     */
    public static String secureXML(String xml) {
        if (xml == null || !xml.contains("assword")) {
            return xml;
        }
        xml = xml.replaceAll(
                "<([a-zA-Z]*[pP])assword>[^<]*</([a-zA-Z]*)assword>",
                "<$1assword>********</$2assword>");
        // attributes: nuxeo-core-auth
        xml = xml.replaceAll("([a-zA-Z]*[pP])assword=\"[^\"]*\"",
                "$1assword=\"********\"");
        // property: default-repository-config
        xml = xml.replaceAll("([a-zA-Z]*[pP])assword\">[^<]*<",
                "$1assword\">********<");
        return xml;
    }

}
