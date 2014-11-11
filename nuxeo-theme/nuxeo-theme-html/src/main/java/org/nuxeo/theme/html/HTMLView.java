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

package org.nuxeo.theme.html;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.fragments.Fragment;
import org.nuxeo.theme.fragments.FragmentType;
import org.nuxeo.theme.rendering.RenderingInfo;
import org.nuxeo.theme.views.TemplateView;
import org.nuxeo.theme.views.ViewType;

public class HTMLView extends TemplateView {

    private static final Log log = LogFactory.getLog(HTMLView.class);

    private static final Pattern firstTagPattern = Pattern.compile(
            "<([a-zA-Z0-9:]*)[^>]*>", Pattern.DOTALL);

    private static final String[] ALLOWED_TAGS = { "html", "body", "table",
            "tr", "td", "div" };

    @Override
    public String render(final RenderingInfo info) {
        final ViewType viewType = getViewType();
        final String template = viewType.getTemplate();
        String result = getTemplateContent(template);

        // Sanity check
        final Matcher matcher = firstTagPattern.matcher(result);
        if (matcher.find()) {
            final String tag = matcher.group(1).toLowerCase();
            boolean found = false;
            for (String allowedTag : ALLOWED_TAGS) {
                if (tag.equals(allowedTag)) {
                    found = true;
                }
            }
            if (!found) {
                // log.warn(String.format(
                // "First HTML tag of view template: %s (<%s>) not one of <html>, <body>, <div>, <table>, <tr>, <td>",
                // template, tag));
            }
        } else {
            log.warn("First HTML tag of view template: " + template
                    + " not found");
        }

        // replace model expressions
        final Element element = info.getElement();
        if (element instanceof Fragment) {
            final Fragment fragment = (Fragment) element;
            if (!info.isDirty() && hasModelExpressions(result)) {
                final FragmentType fragmentType = fragment.getFragmentType();
                String fragmentTypeName = fragmentType.getTypeName();
                log.warn(String.format(
                        "Cannot render \"%s\" with the view \"%s\" because the view's template contains a 'nxthemesInfo' variable and the fragment type is not declared as dynamic. Try to add <dynamic>true</dynamic> in the <fragment name=\"%s\"> definition.",
                        fragmentTypeName, getViewType().getViewName(),
                        fragmentTypeName));
                return "";
            }
            result = replaceModelExpressions(info, result);
        }

        // replace [nxthemes markup] strings with the actual markup
        result = result.replace("[nxthemes markup]", info.getMarkup());

        return result;
    }

    public String replaceModelExpressions(
            @SuppressWarnings("unused") final RenderingInfo info,
            final String html) {
        return html;
    }

    public boolean hasModelExpressions(final String html) {
        return html.contains("nxthemesInfo");
    }

}
