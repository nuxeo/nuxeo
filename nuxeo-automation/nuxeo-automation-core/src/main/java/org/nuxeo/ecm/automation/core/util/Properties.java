/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.core.Constants;

/**
 * Inline properties file content. This class exists to have a real type for
 * parameters accepting properties content.
 *
 * @see Constants
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Properties extends HashMap<String, String> {

    private static final long serialVersionUID = 1L;

    public Properties() {
    }

    public Properties(int size) {
        super(size);
    }

    public Properties(Map<String, String> props) {
        super(props);
    }

    public Properties(String content) throws Exception {
        StringReader reader = new StringReader(content);
        loadProperties(reader, this);
    }

    public static Map<String, String> loadProperties(Reader reader)
            throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        loadProperties(reader, map);
        return map;
    }

    public static void loadProperties(Reader reader, Map<String, String> map)
            throws Exception {
        BufferedReader in = new BufferedReader(reader);
        String line = in.readLine();
        String prevLine = null;
        while (line != null) {
            line = line.trim();
            if (line.startsWith("#") || line.length() == 0) {
                prevLine = null;
                line = in.readLine();
                continue;
            }
            if (line.endsWith("\\")) {
                line = line.substring(0, line.length() - 1);
                prevLine = prevLine != null ? prevLine + line : line;
                line = in.readLine();
                continue;
            }
            if (prevLine != null) {
                line = prevLine + line;
            }
            prevLine = null;
            setPropertyLine(map, line);
            line = in.readLine();
        }
        if (prevLine != null) {
            setPropertyLine(map, prevLine);
        }
    }

    protected static void setPropertyLine(Map<String, String> map, String line)
            throws Exception {
        int i = line.indexOf('=');
        if (i == -1) {
            throw new IOException("Invalid property line: " + line);
        }
        map.put(line.substring(0, i).trim(), line.substring(i + 1).trim());
    }

}
