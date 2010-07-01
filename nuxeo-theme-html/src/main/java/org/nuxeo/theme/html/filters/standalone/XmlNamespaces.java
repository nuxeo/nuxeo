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

package org.nuxeo.theme.html.filters.standalone;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.theme.rendering.RenderingInfo;
import org.nuxeo.theme.rendering.StandaloneFilter;

public final class XmlNamespaces extends StandaloneFilter {

    static final Pattern tagsPattern = Pattern.compile(
            "(.*?)<([^/@!<>].*?)>(.*)", Pattern.DOTALL);

    static final String xmlnsAttrStr = "xmlns[^\"]+\"([^\"]+)\"";

    static final String spaceXmlnsAttrStr = " " + xmlnsAttrStr;

    static final Pattern xmlnsAttrPattern = Pattern.compile(xmlnsAttrStr,
            Pattern.DOTALL);

    @Override
    public RenderingInfo process(final RenderingInfo info, final boolean cache) {
        String markup = info.getMarkup();

        final Matcher attrMatcher = xmlnsAttrPattern.matcher(markup);
        if (!attrMatcher.find()) {
            return info;
        }

        final StringBuilder s = new StringBuilder();
        do {
            final String attr = attrMatcher.group(0);
            if (s.indexOf(attr) < 0) {
                s.append(' ');
                s.append(attr);
            }
        } while (attrMatcher.find());

        // remove existing uid attributes, if any
        markup = markup.replaceAll(spaceXmlnsAttrStr, "");

        final Matcher firstMatcher = tagsPattern.matcher(markup);
        if (!firstMatcher.find()) {
            return info;
        }

        // write the final markup
        final String f = String.format("%s<%s%s>%s", firstMatcher.group(1),
                firstMatcher.group(2), s.toString(), firstMatcher.group(3));

        info.setMarkup(f);
        return info;
    }

}
