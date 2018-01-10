/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.runtime.deployment.preprocessor.template;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.TextTemplate;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Template {

    private static final Log log = LogFactory.getLog(Template.class);

    public static final String BEGIN = "BEGIN";

    public static final String END = "END";

    // we should use a linked hash map to preserve the
    // insertion order when iterating over the elements in the map
    final LinkedHashMap<String, Part> parts;

    public Template() {
        parts = new LinkedHashMap<>();
    }

    public void addPart(String name, String text) {
        parts.put(name, new Part(name, text));
    }

    public void update(TemplateContribution tc, Map<String, String> ctx) {
        String content = tc.getContent();
        content = new TextTemplate(ctx).processText(content);
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
            log.debug("Could not find marker: " + marker);
        }
    }

    public void prependText(String marker, String text) {
        Part part = parts.get(marker);
        if (part != null) {
            part.prepend(text);
        } else {
            log.debug("Could not find marker: " + marker);
        }
    }

    public void replaceText(String marker, String text) {
        Part part = parts.get(marker);
        if (part != null) {
            part.replace(text);
        } else {
            log.debug("Could not find marker: " + marker);
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

        public void append(String aText) {
            text.append(aText);
        }

        public void prepend(String aText) {
            text.insert(offset, aText);
        }

        public void replace(String aText) {
            text.replace(offset, text.length(), aText);
        }

        public String getText() {
            return text.toString();
        }

        public String getName() {
            return name;
        }

    }

}
