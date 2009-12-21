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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: DummyIOResourceAdapter.java 25715 2007-10-05 16:12:07Z dmihalache $
 */

package org.nuxeo.ecm.platform.io.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.platform.io.api.IOResourceAdapter;
import org.nuxeo.ecm.platform.io.api.IOResources;

/**
 * IO resource adapter for tests
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class DummyIOResourceAdapter implements IOResourceAdapter {

    private static final long serialVersionUID = 5411716618665857482L;

    // backend for tests: key are document refs, values are dummy corresponding
    // values
    public static final Map<String, String> backend = new HashMap<String, String>();

    private Map<String, Serializable> properties;

    public IOResources extractResources(String repo,
            Collection<DocumentRef> sources) {
        Map<String, String> extracted = new HashMap<String, String>();
        for (DocumentRef ref : sources) {
            String key = ref.toString();
            if (backend.containsKey(key)) {
                extracted.put(key, backend.get(key));
            }
        }
        return new DummyIOResources(extracted);
    }

    public IOResources translateResources(String repo,
            IOResources resources, DocumentTranslationMap map) {
        if (!(resources instanceof DummyIOResources)) {
            return null;
        }
        Map<String, String> givenResources = ((DummyIOResources) resources).getResources();
        Map<String, String> translated = new HashMap<String, String>();
        for (Map.Entry<DocumentRef, DocumentRef> cor : map.getDocRefMap().entrySet()) {
            String oldKey = cor.getKey().toString();
            if (givenResources.containsKey(oldKey)) {
                String newKey = cor.getValue().toString();
                translated.put(newKey, givenResources.get(oldKey));
            }
        }
        return new DummyIOResources(translated);
    }

    public void getResourcesAsXML(OutputStream out, IOResources resources) {
        if (!(resources instanceof DummyIOResources)) {
            return;
        }
        Map<String, String> givenResources = ((DummyIOResources) resources).getResources();
        try {
            out.write("<?xml version=\"1.0\">\n".getBytes());
            for (Map.Entry<String, String> entry : givenResources.entrySet()) {
                String xml = String.format("<dummy for=\"%s\">%s</dummy>\n",
                        entry.getKey(), entry.getValue());
                out.write(xml.getBytes());
            }
            out.close();
        } catch (IOException e) {
        }
    }

    public IOResources loadResourcesFromXML(InputStream stream) {
        Map<String, String> resources = new HashMap<String, String>();
        BufferedReader reader = null;
        Pattern pattern = Pattern.compile("<dummy for=\"([a-zA-Z_0-9\\-]+)\">([a-zA-Z_0-9\\-]+)</dummy>");
        try {
            reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m = pattern.matcher(line);
                if (m.matches()) {
                    resources.put(m.group(1), m.group(2));
                }
            }
        } catch (IOException e) {
        } finally {
            // shouldn't close here as this will close also the underlying stream
            // and we don't want because there might be more to read??
            if (false && reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return new DummyIOResources(resources);
    }

    public void storeResources(IOResources resources) {
        if (!(resources instanceof DummyIOResources)) {
            return;
        }
        Map<String, String> givenResources = ((DummyIOResources) resources).getResources();
        for (Map.Entry<String, String> entry : givenResources.entrySet()) {
            backend.put(entry.getKey(), entry.getValue());
        }
    }

    public Map<String, Serializable> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Serializable> properties) {
        this.properties = properties;
    }

}
