/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.theme.editor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StyleFieldProperty {

    private final String name;

    private final String value;

    private final String type;

    private final String id;

    private static final Pattern cssChoicePattern = Pattern.compile("\\[(.*?)\\]");

    public StyleFieldProperty(String name, String value, String type, String id) {
        this.name = name;
        // escape quotes (used internally to represent presets)
        this.value = value.replace("\"", "&quot;");
        this.type = type;
        this.id = id;
    }

    public String getName() {
        return String.format("&quot;%s&quot;", name);
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return name;
    }

    public String getInputWidget() {
        final StringBuilder rendered = new StringBuilder();

        final Matcher choiceMatcher = cssChoicePattern.matcher(type);

        final boolean hasChoices = choiceMatcher.find();

        if (hasChoices) {
            // render selection list
            String choices = choiceMatcher.group(1);
            rendered.append(String.format(
                    "<select id=\"%s\" name=\"property:%s\">", id, name));
            rendered.append("<option></option>");
            for (String choice : choices.split("\\|")) {
                rendered.append(String.format("<option%s>%s</option>",
                        choice.equals(value) ? " selected=\"selected\"" : "",
                        choice));
            }
            rendered.append("</select>");
        } else {
            // render input area
            String input = String.format(
                    "<input id=\"%s\" type=\"text\" class=\"textInput\" name=\"property:%s\" value=\"%s\" />",
                    id, name, value);
            rendered.append(input);
        }

        String category = (String) Utils.getStylePreviewCategories().get(name);
        if (category != null) {
            // add a style picker
            rendered.append(String.format(
                    "<input id=\"%s\" type=\"button\" class=\"picker\" property=\"%s\" category=\"%s\" value=\"\" />",
                    id, name, category));
        }

        return rendered.toString();
    }

    public String getId() {
        return id;
    }

}
