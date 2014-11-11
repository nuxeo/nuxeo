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

package org.nuxeo.theme.editor.filters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.theme.rendering.RenderingInfo;
import org.nuxeo.theme.rendering.StandaloneFilter;

public final class ElementUid extends StandaloneFilter {

    static final Pattern firstTagPattern = Pattern.compile("<(.*?)>",
            Pattern.DOTALL);

    static final Pattern otherTagsPattern = Pattern.compile("<.*?>(.*)",
            Pattern.DOTALL);

    @Override
    public RenderingInfo process(final RenderingInfo info, final boolean cache) {
        if (info.isRenderingPostponed(cache)) {
            return info;
        }
        final String markup = info.getMarkup();
        final Matcher firstMatcher = firstTagPattern.matcher(markup);
        final Matcher othersMatcher = otherTagsPattern.matcher(markup);

        if (!(firstMatcher.find() && othersMatcher.find())) {
            return info;
        }

        String inBrackets = firstMatcher.group(1);

        // remove existing uid attributes, if any
        inBrackets = inBrackets.replaceAll(" id=\"(.*?)\"", "");

        // write the final markup
        String f = "";

        if (inBrackets.endsWith("/")) {
            f = String.format("<%s id=\"e%s\" />%s",
                    inBrackets.replaceAll("/$", "").trim(),
                    info.getElement().getUid(), othersMatcher.group(1));
        } else {
            f = String.format("<%s id=\"e%s\">%s", inBrackets,
                    info.getElement().getUid(), othersMatcher.group(1));
        }

        info.setMarkup(f);
        return info;
    }

}
