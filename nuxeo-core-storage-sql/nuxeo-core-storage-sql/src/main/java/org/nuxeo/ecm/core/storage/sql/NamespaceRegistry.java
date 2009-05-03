/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.storage.StorageException;

/**
 * The global Namespace Registry.
 *
 * @author Florent Guillaume
 */
public class NamespaceRegistry {

    private static final Map<String, String> DEFAULT_MAPPING;

    static {
        Map<String, String> map = new HashMap<String, String>();
        map.put("", "");
        map.put("xml", "http://www.w3.org/XML/1998/namespace");
        map.put("xmlns", "http://www.w3.org/2000/xmlns/");
        map.put("jcr", "http://www.jcp.org/jcr/1.0");
        map.put("nt", "http://www.jcp.org/jcr/nt/1.0");
        map.put("mix", "http://www.jcp.org/jcr/mix/1.0");
        map.put("sv", "http://www.jcp.org/jcr/sv/1.0");
        DEFAULT_MAPPING = Collections.unmodifiableMap(map);
    }

    private final Map<String, String> prefixToURI;

    private final Map<String, String> uriToPrefix;

    public NamespaceRegistry() {
        prefixToURI = new HashMap<String, String>();
        uriToPrefix = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : DEFAULT_MAPPING.entrySet()) {
            String prefix = entry.getKey();
            String uri = entry.getValue();
            prefixToURI.put(prefix, uri);
            uriToPrefix.put(uri, prefix);
        }
    }

    public String getPrefix(String uri) throws StorageException {
        String prefix = uriToPrefix.get(uri);
        if (prefix == null) {
            throw new StorageException(uri);
        }
        return prefix;
    }

    public String[] getPrefixes() {
        return (String[]) prefixToURI.keySet().toArray();
    }

    public String getURI(String prefix) throws StorageException {
        String uri = prefixToURI.get(prefix);
        if (uri == null) {
            throw new StorageException(prefix);
        }
        return uri;
    }

    public String[] getURIs() {
        return (String[]) uriToPrefix.keySet().toArray();
    }

    public void registerNamespace(String prefix, String uri)
            throws StorageException {
        if (prefix == null) {
            throw new IllegalArgumentException("Illegal null prefix");
        }
        if (uri == null) {
            throw new IllegalArgumentException("Illegal null uri");
        }
        if (DEFAULT_MAPPING.containsKey(prefix)) {
            if (DEFAULT_MAPPING.get(prefix).equals(uri)) {
                // identical registration, ignore
                return;
            }
            throw new StorageException(
                    "Cannot redefine namespace of system prefix '" + prefix
                            + "'");
        }
        if (DEFAULT_MAPPING.containsValue(uri)) {
            throw new StorageException(
                    "Cannot redefine prefix of system namespace '" + uri + "'");
        }
        if (prefix.toLowerCase().startsWith("xml")) {
            throw new StorageException(
                    "Cannot define namespace for xml prefix '" + prefix + "'");
        }

        String oldPrefix = uriToPrefix.get(uri);
        if (prefix.equals(oldPrefix)) {
            // identical mapping, ignore
            return;
        }
        if (prefixToURI.containsKey(prefix)) {
            // XXX redefining prefix, we would have to check in all the database
            // to see if the namespace is used...
            throw new StorageException("Reregistering prefix '" + prefix
                    + "' is not supported");
        }
        if (oldPrefix != null) {
            // XXX disabled until the impact in the database is known
            throw new StorageException("Cannot reregister namespace '" + uri
                    + "'");
            // prefixToURI.remove(oldPrefix);
            // uriToPrefix.remove(uri);
        }
        prefixToURI.put(prefix, uri);
        uriToPrefix.put(uri, prefix);
    }

    public void unregisterNamespace(String prefix) throws StorageException {
        if (prefix == null) {
            throw new IllegalArgumentException("Illegal null prefix");
        }
        if (DEFAULT_MAPPING.containsKey(prefix)) {
            throw new StorageException("Cannot unregister system prefix '"
                    + prefix + "'");
        }
        // XXX we would have to check in all the database to see if the
        // namespace is used...
        throw new StorageException("Unregistering prefix '" + prefix
                + "' is not supported");
    }

}
