/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.api.externalblob;

import java.util.Collections;
import java.util.Map;

import org.nuxeo.ecm.core.api.model.PropertyException;

/**
 * Abstract class for external blob adapters.
 * <p>
 * Provides generic methods
 * <p>
 * Extend this class if you want your contributions to be robust to interface
 * changes.
 *
 * @author Anahide Tchertchian
 */
public abstract class AbstractExternalBlobAdapter implements
        ExternalBlobAdapter {

    protected String prefix;

    protected Map<String, String> properties;

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Map<String, String> getProperties() {
        // de-reference
        if (properties == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public String getProperty(String name) {
        Map<String, String> props = getProperties();
        String prop = props.get(name);
        prop = prop.trim();
        return prop;
    }

    @Override
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public String getLocalName(String uri) throws PropertyException {
        String prefix = getPrefix();
        if (prefix == null) {
            throw new PropertyException(String.format(
                    "Null prefix on external blob adapter with class '%s'",
                    getClass().getName()));
        }
        if (uri == null
                || !uri.startsWith(prefix
                        + ExternalBlobAdapter.PREFIX_SEPARATOR)) {
            throw new PropertyException(
                    String.format(
                            "Invalid uri '%s' for this adapter: expected to start with prefix '%s'",
                            uri, prefix));
        }
        return uri.substring(prefix.length() + 1);
    }
}
