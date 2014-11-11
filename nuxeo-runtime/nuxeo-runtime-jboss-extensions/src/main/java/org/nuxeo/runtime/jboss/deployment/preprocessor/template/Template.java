/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.runtime.jboss.deployment.preprocessor.template;

import java.util.LinkedHashMap;
import java.util.Map;

import org.nuxeo.common.utils.TextTemplate;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Template {

    public static final String BEGIN = "BEGIN";
    public static final String END = "END";

    // we should use a linked hash map to preserve the
    // insertion order when iterating over the elements in the map
    final LinkedHashMap<String, Part> parts;

    public Template() {
        parts = new LinkedHashMap<String, Part>();
    }

    public void addPart(String name, String text) {
        parts.put(name, new Part(name, text));
    }

    public void update(TemplateContribution tc, Map<String, String> ctx) {
        String content = new TextTemplate(ctx).process(tc.getContent());
        if (tc.isAppending()) {
            appendText(tc.getMarker(), content);
        } else if (tc.isPrepending()) {
            prependText(tc.getMarker(), content);
        } else if (tc.isReplacing()) {
            replaceText(tc.getMarker(), content);
        }
    }

    public void appendText(String marker, String text) {
        Part part = parts.get(marker);
        if (part != null) {
            part.append(text);
        } else {
            //TODO
            System.out.println("TODO >>>>>>>>>>>>> could not finnd marker " + marker);
        }
    }

    public void prependText(String marker, String text) {
        Part part = parts.get(marker);
        if (part != null) {
            part.prepend(text);
        } else {
            //TODO
            System.out.println("TODO >>>>>>>>>>>>> could not find marker " + marker);
        }
    }

    public void replaceText(String marker, String text) {
        Part part = parts.get(marker);
        if (part != null) {
            part.replace(text);
        } else {
            //TODO
            System.out.println("TODO >>>>>>>>>>>>> could not find marker " + marker);
        }
    }

    public String getText() {
        StringBuilder buf = new StringBuilder();
        for (Part part : parts.values()) {
            buf.append(part.text);
        }
        return buf.toString();
    }

    static class Part {
        public final String name; // the name of the part used in markers
        public final StringBuffer text; // the text before the marker
        public final int offset; // the initial length of the text

        Part(String name, String text) {
            this.name = name;
            this.text = text == null ? new StringBuffer() : new StringBuffer(text);
            offset = this.text.length();
        }

        public void append(String text) {
            this.text.append(text);
        }

        public void prepend(String text) {
            this.text.insert(offset, text);
        }

        public void replace(String text) {
            this.text.replace(offset, this.text.length(), text);
        }

        public String getText() {
            return text.toString();
        }

        public String getName() {
            return name;
        }

    }

}
