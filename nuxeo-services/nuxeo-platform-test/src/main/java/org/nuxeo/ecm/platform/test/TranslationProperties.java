/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Properties implementation detecting duplicate keys, that does *not* handle properly removal of items.
 *
 * @since 7.3
 */
public class TranslationProperties extends Properties {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(TranslationProperties.class);

    protected Properties duplicates;

    protected Set<String> singleLabels;

    public TranslationProperties() {
        super();
        duplicates = new Properties();
        singleLabels = new HashSet<String>();
    }

    /**
     * Override to detect duplicate keys
     */
    @SuppressWarnings("unchecked")
    @Override
    public synchronized Object put(Object key, Object value) {
        if (key instanceof String && !((String) key).contains(".")) {
            singleLabels.add((String) key);
        }
        if (containsKey(key)) {
            List<Object> values;
            if (duplicates.containsKey(key)) {
                values = (List<Object>) duplicates.get(key);
            } else {
                values = new ArrayList<>();
                duplicates.put(key, values);
                values.add(get(key));
            }
            values.add(value);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Detected duplicate key '%s', values=%s", key, values));
            }
        }
        return super.put(key, value);
    }

    public Set<String> getDuplicatePropertyNames() {
        return duplicates.stringPropertyNames();
    }

    public Properties getDuplicates() {
        return duplicates;
    }

    public Set<String> getSingleLabels() {
        return singleLabels;
    }

    @Override
    public synchronized void clear() {
        super.clear();
        duplicates.clear();
    }

    // other removal methods not handled

}