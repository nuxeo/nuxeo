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
 */
package org.nuxeo.ecm.automation.client.jaxrs.model;

import java.util.Date;

/**
 * A immutable document. You cannot modify documents. Documents are as they are
 * returned by the server. To modify documents use operations.
 * 
 * You need to create your own wrapper if you need to access the document
 * properties in a multi-level way. This is a flat representation of the
 * document.
 * 
 * Possible property value types:
 * <ul>
 * <li>String
 * <li>Number
 * <li>Date
 * <ul>
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class Document implements OperationInput {

    protected String id;

    protected String path;

    protected String type;

    // TODO can be stored in map

    protected String state;

    protected String lock;

    protected PropertyMap properties;

    /**
     * Reserved to framework. Should be only called by client framework when
     * unmarshalling documents
     * 
     * @param id
     * @param type
     * @param path
     * @param properties
     */
    public Document(String id, String type, String path, String state,
            String lock, PropertyMap properties) {
        this.id = id;
        this.path = path;
        this.type = type;
        this.state = state;
        this.lock = lock;
        this.properties = properties == null ? new PropertyMap() : properties;
    }

    public String getInputType() {
        return "document";
    }

    public String getId() {
        return id;
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

    public boolean isBinary() {
        return false;
    }

    public String getInputRef() {
        return "doc:" + id;
    }

    public String toString() {
        return id;
    }

}
