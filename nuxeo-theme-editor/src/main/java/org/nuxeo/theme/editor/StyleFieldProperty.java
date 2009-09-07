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

import org.nuxeo.theme.themes.ThemeManager;



/*
 * (C) Copyright 2006-2009 Nuxeo SAS <http://nuxeo.com> and others
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

public class StyleFieldProperty {

    private final String name;

    private final String value;

    private final String type;

    private static final Pattern cssChoicePattern = Pattern.compile("\\[(.*?)\\]");

    public StyleFieldProperty(String name, String value, String type) {
        this.name = name;
        // escape quotes (used internally to represent presets)
        this.value = value.replace("\"", "&quot;");
        this.type = type;
    }

    public String getName() {
        return String.format("&quot;%s&quot;", name);
    }

    public String getValue() {
        return value;
    }

    public String getRendered() {
        final StringBuilder rendered = new StringBuilder();
        final String label = name;
        rendered.append("<label>").append(label).append("</label>");

        final Matcher choiceMatcher = cssChoicePattern.matcher(type);

        final boolean hasChoices = choiceMatcher.find();
        final String category = ThemeManager.getPreviewCategoryForProperty(name);

        if (hasChoices) {
            // render selection list
            String choices = choiceMatcher.group(1);
            rendered.append(String.format("<select name=\"property:%s\">", name));
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
                    "<input type=\"text\" class=\"textInput\" name=\"property:%s\" value=\"%s\" />",
                    name, value);
            rendered.append(input);
        }

        if (category != null) {
            // add a style picker
            rendered.append(String.format(
                    "<input type=\"button\" class=\"picker\" property=\"%s\" category=\"%s\" value=\"\" />",
                    name, category));
        }

        return rendered.toString();
    }
}
