/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.resolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.WebContext;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.exceptions.WebSecurityException;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentMapper implements DocumentResolver {

    public final static String MAPPINGS_FILE = "roots.mapping";

    protected WebEngine engine;

    protected final ConcurrentMap<String, String[]> mappings; // appId:path -> entry
    protected ConcurrentMap<String, List<String[]>> reverseMapping; // docPath -> entry


    public DocumentMapper(WebEngine engine) throws IOException {
        this.engine = engine;
        mappings = new ConcurrentHashMap<String, String[]>();
        reverseMapping = new ConcurrentHashMap<String, List<String[]>>();
        load();
    }

    public DocumentModel getDocument(WebContext ctx, String path) throws WebException {
        String[] entry = null;
        if (ctx == null) { // no context - get only global mappings
            String key = new StringBuilder().append("*:").append(path).toString();
            entry = mappings.get(key);
        } else {
            String key = new StringBuilder(ctx.getApplication().getId()).append(':').append(path).toString();
            entry = mappings.get(key);
            if (entry == null) { // search in global mappings
                key = new StringBuilder().append("*:").append(path).toString();
                entry = mappings.get(key);
            }
        }
        if (entry != null) {
            String docPath = entry[2];
            if (docPath != null) {
                try {
                    return ctx.getCoreSession().getDocument(new PathRef(docPath));
                } catch (DocumentSecurityException e) {
                    throw new WebSecurityException("Failed to get document bound to "+path, e);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    public void addMapping(String app, String path, String docPath) {
        String key = app == null ? path : new StringBuilder(app).append(":").append(path).toString();
        String[] entry = new String[] {app, path, docPath};
        mappings.put(key, entry);
        reverseMapping.remove(docPath);
    }

    public void removeMapping(String app, String path, String docPath) {
        String key = app == null ? path : new StringBuilder(app).append(":").append(path).toString();
        mappings.remove(key);
        reverseMapping.remove(docPath);
    }

    /**
     * This method is used for information (in administration consoles) and calling it may be costly
     * @param doc
     * @return
     */
    public List<String[]> getMappingsForDocument(String docPath) {
        List<String[]> mappings = reverseMapping.get(docPath);
        if (mappings == null) {
            mappings = findMappingsForDocument(docPath);
            reverseMapping.putIfAbsent(docPath, mappings);
        } // rebuild reverse mappings
        return (List<String[]>)new ArrayList<String[]>(mappings); // make a copy
    }

    protected List<String[]> findMappingsForDocument(String docPath) {
        ArrayList<String[]> result = new ArrayList<String[]>();
        String[][] values = mappings.values().toArray(new String[mappings.size()][]);
        for (String[] entry : values) {
            if (docPath.equals(entry[2])) {
                result.add(entry);
            }
        }
        return result;
    }


    protected File getMappingsFile() {
        return new File (engine.getRootDirectory(), MAPPINGS_FILE);
    }

    public synchronized void load() throws IOException {
        try {
            List<String[]> storedMappings = readMappingsFile();
            for (String[] entry : storedMappings) {
                String key = new StringBuilder().append(entry[0]).append(':').append(entry[1]).toString();
                mappings.put(key, entry);
            }
            reverseMapping = new ConcurrentHashMap<String, List<String[]>>();
        } catch (FileNotFoundException e) {
            // do nothing
        }
    }

    public synchronized void store() throws IOException {
        String eol = System.getProperty("line.separator");
        StringBuilder content = new StringBuilder(4096);
        content.append("#This is a generated file. The format is: appId:pathInfo:docPath").append(eol);
        String[][] ar = mappings.values().toArray(new String[mappings.size()][]);
        for (String[] entry : ar) {
            content.append(entry[0]).append(':').append(entry[1]).append(':').append(entry[2]).append(eol);
        }
        writeMappingsFile(content.toString());
    }

    protected  void writeMappingsFile(String content) throws IOException {
        FileWriter out = new FileWriter(getMappingsFile());
        try {
            out.write(content);
        } finally {
            out.close();
        }
    }

    protected  List<String[]> readMappingsFile() throws IOException {
        ArrayList<String[]> result = new ArrayList<String[]>();
        BufferedReader in = new BufferedReader(new FileReader(getMappingsFile()));
        try {
            String line = in.readLine();
            while (line != null) {
                line = line.trim();
                if (line.length() != 0 && line.charAt(0) != '#') {
                    String[] entry = StringUtils.split(line, ':', true);
                    if (entry.length != 3) {
                        System.err.println("Invalid entry in mappings file: "+line);
                        continue;
                    }
                    result.add(entry);
                }
                line = in.readLine();
            }
        } finally {
            in.close();
        }
        return result;
    }


}
