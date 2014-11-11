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
package org.nuxeo.ecm.automation.client.jaxrs.model;

import java.util.Date;

/**
 * A immutable document. You cannot modify documents. Documents are as they are
 * returned by the server. To modify documents use operations.
 * <p>
 * You need to create your own wrapper if you need to access the document
 * properties in a multi-level way. This is a flat representation of the
 * document.
 * <p>
 * Possible property value types:
 * <ul>
 * <li>String
 * <li>Number
 * <li>Date
 * <ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Document extends DocRef {

    protected final String path;

    protected final String type;

    // TODO can be stored in map

    protected final String state;

    protected final String lock;

    protected final PropertyMap properties;

    /**
     * Reserved to framework. Should be only called by client framework when
     * unmarshalling documents.
     */
    public Document(String id, String type, String path, String state,
            String lock, PropertyMap properties) {
        super(id);
        this.path = path;
        this.type = type;
        this.state = state;
        this.lock = lock;
        this.properties = properties == null ? new PropertyMap() : properties;
    }

    public String getId() {
        return ref;
    }

    @Override
    public String getInputType() {
        return "document";
    }

    public String getPath() {
        return path;
    }

    public String getType() {
        return type;
    }

    public String getLock() {
        return lock;
    }

    public String getState() {
        return state;
    }

    public Date getLastModified() {
        return properties.getDate("dc:modified");
    }

    public String getTitle() {
        return properties.getString("dc:title");
    }

    public PropertyMap getProperties() {
        return properties;
    }

    public String getString(String key) {
        return properties.getString(key);
    }

    public Date getDate(String key) {
        return properties.getDate(key);
    }

    public Long getLong(String key) {
        return properties.getLong(key);
    }

    public Double getDouble(String key) {
        return properties.getDouble(key);
    }

    public String getString(String key, String defValue) {
        return properties.getString(key, defValue);
    }

    public Date getDate(String key, Date defValue) {
        return properties.getDate(key, defValue);
    }

    public Long getLong(String key, Long defValue) {
        return properties.getLong(key, defValue);
    }

    public Double getDouble(String key, Double defValue) {
        return properties.getDouble(key, defValue);
    }

    public void set(String key, String defValue) {
        properties.set(key, defValue);
    }

    public void set(String key, Date defValue) {
        properties.set(key, defValue);
    }

    public void set(String key, Long defValue) {
        properties.set(key, defValue);
    }

    public void set(String key, Double defValue) {
        properties.set(key, defValue);
    }

}
