/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.rendering;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.ecm.platform.rendering.wiki.WikiFilter;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebContext;


public class WikiPageLinkResolver implements WikiFilter{

    // TODO get this from config files
    static final String PATTERN = "([A-Z]+[a-z]+[A-Z][A-Za-z]*.)?([A-Z]+[a-z]+[A-Z][A-Za-z]*)";
    static final Pattern PAGE_LINKS_PATTERN = Pattern.compile(PATTERN);

    static final String LINK_TEMPLATE = "<a  href=\"%s\" class=\"%s\">%s</a>";

    public String apply(String content) {
        Matcher m = PAGE_LINKS_PATTERN.matcher(content);
        StringBuffer sb = new StringBuffer();
        if (!m.find()) {
            return content;
        }
        do {
            String s = m.group();
            String link = buildLink(s);
            m.appendReplacement(sb, link);
        } while (m.find());
        m.appendTail(sb);
        return sb.toString();
    }

    protected String buildLink(String pageName) {
        String href = null;
        if (pageName.contains(".")){ // WikiName.WikiPage case
            href = "../"+pageName.replace(".", "/");
        } else {
            href = "./"+pageName;
        }
        // TOdO check if page exists
        return String.format(LINK_TEMPLATE, href, "", pageName);
    }



}
