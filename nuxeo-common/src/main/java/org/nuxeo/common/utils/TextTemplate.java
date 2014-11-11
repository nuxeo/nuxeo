/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu, jcarsique
 *
 * $Id$
 */

package org.nuxeo.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO Please document me.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TextTemplate {

    private static final Pattern PATTERN = Pattern.compile("\\$\\{([a-zA-Z_0-9\\-\\.]+)\\}");

    private final Properties vars;

    public TextTemplate() {
        vars = new Properties();
    }

    /**
     * @deprecated prefer use of {@link #TextTemplate(Properties)}
     */
    public TextTemplate(Map<String, String> vars) {
        this.vars = new Properties();
        this.vars.putAll(vars);
    }

    /**
     * @param vars Properties containing keys and values for template processing
     */
    public TextTemplate(Properties vars) {
        this.vars = vars;
    }

    /**
     * @deprecated prefer use of {@link #getVariables()} then {@link Properties}
     *             .load()
     */
    public void setVariables(Map<String, String> vars) {
        this.vars.putAll(vars);
    }

    public void setVariable(String name, String value) {
        vars.setProperty(name, value);
    }

    public String getVariable(String name) {
        return vars.getProperty(name);
    }

    public Properties getVariables() {
        return vars;
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

    /**
     * Recursive call {@link #process(InputStream, OutputStream)} on each file
     * from "in" directory to "out" directory
     *
     * @param in Directory to read files from
     * @param out Directory to write files to
     */
    public void processDirectory(File in, File out)
            throws FileNotFoundException, IOException {
        if (in.isFile()) {
            if (out.isDirectory()) {
                out = new File(out, in.getName());
            }
            FileInputStream is = null;
            FileOutputStream os = new FileOutputStream(out);
            try {
                is = new FileInputStream(in);
                process(is, os);
            } finally {
                if (is != null) {
                    is.close();
                }
                os.close();
            }
        } else if (in.isDirectory()) {
            if (!out.exists()) {
                // allow renaming destination directory
                out.mkdirs();
            } else if (!out.getName().equals(in.getName())) {
                // allow copy over existing arborescence
                out = new File(out, in.getName());
                out.mkdir();
            }
            for (File file : in.listFiles()) {
                processDirectory(file, out);
            }
        }
    }

}
