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

package org.nuxeo.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: doc
/**
 * Please document me.
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TextTemplate {

    private static final Pattern PATTERN = Pattern.compile("\\$\\{([a-zA-Z_0-9\\-\\.]+)\\}");

    private final Map<String, String> vars = new HashMap<String, String>();


    public TextTemplate() {
    }

    public TextTemplate(Map<String, String> vars) {
        this.vars.putAll(vars);
    }

    public void setVariables(Map<String, String> vars) {
        this.vars.putAll(vars);
    }

    public Map<String, String> getVariables() {
        return vars;
    }

    public void setVariable(String name, String value) {
        vars.put(name, value);
    }

    public String getVariable(String name) {
        return vars.get(name);
    }

    public String process(CharSequence text) {
        Matcher m = PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String var = m.group(1);
            String value = getVariable(var);
            if (value != null) {
                m.appendReplacement(sb, value);
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public String process(InputStream in) throws IOException {
        String text = FileUtils.read(in);
        return process(text);
    }

    public void process(InputStream in, OutputStream out) throws IOException {
        String text = FileUtils.read(in);
        out.write(process(text).getBytes());
    }

}
