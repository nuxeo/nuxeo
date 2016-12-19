/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */
package org.nuxeo.runtime.deployment.preprocessor.template;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TemplateParser {

    // Utility class.
    private TemplateParser() {
    }

    public static Template parse(File file) throws IOException {
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))){
            return parse(in);
        }
    }

    public static Template parse(URL url) throws IOException {
        try (InputStream in = new BufferedInputStream(url.openStream())){
            return parse(in);
        }
    }

    public static Template parse(InputStream in) throws IOException {
        String s = IOUtils.toString(in, Charsets.UTF_8);
        return parse(s.toCharArray());
    }

    public static Template parse(char[] chars) {
        Template tpl = new Template();
        StringBuilder buf = new StringBuilder();
        StringBuilder name = new StringBuilder();

        // add the begin part
        tpl.addPart(Template.BEGIN, null);

        boolean marker = false;
        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            switch (ch) {
            case '%':
                if (i < chars.length && chars[i + 1] == '{') {
                    marker = true;
                    i++;
                } else {
                    if (marker) {
                        name.append(ch);
                    } else {
                        buf.append(ch);
                    }
                }
                break;
            case '}':
                if (i < chars.length && chars[i + 1] == '%') {
                    marker = false;
                    i++;
                    // create a new Part:
                    tpl.addPart(name.toString(), buf.toString());
                    name.setLength(0);
                    buf.setLength(0);
                } else {
                    if (marker) {
                        name.append(ch);
                    } else {
                        buf.append(ch);
                    }
                }
                break;
            default:
                if (marker) {
                    name.append(ch);
                } else {
                    buf.append(ch);
                }
                break;
            }
        }

        // create the END part
        tpl.addPart(Template.END, buf.toString());

        return tpl;
    }

}
