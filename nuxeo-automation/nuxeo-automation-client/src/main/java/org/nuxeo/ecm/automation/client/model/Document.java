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
package org.nuxeo.ecm.automation.client.model;

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

    private static final long serialVersionUID = 1L;

    protected final String repository;

    protected final String path;

    protected final String type;

    // TODO can be stored in map

    protected final String state;

    protected final String lockOwner;
    
    protected final String lockCreated;

    protected final String versionLabel;

    protected final PropertyMap properties;

    protected final PropertyMap contextParameters;

    protected final String changeToken;

    protected final PropertyList facets;

    @Deprecated
    /**
     * Deprecated now use with the constructor with versionLabel
     *
     */
    public Document(String id, String type, PropertyList facets,
            String changeToken, String path, String state, String lockOwner,
            String lockCreated, String repository, PropertyMap properties,
            PropertyMap contextParameters) {
        this(id,  type,  facets,
                 changeToken,  path,  state,  lockOwner,
                 lockCreated,  repository,  null, properties, contextParameters);
    }

    /**
     * Reserved to framework. Should be only called by client framework when
     * unmarshalling documents.
     * @since 5.7
     * @since 5.6-HF17
     */
    public Document(String id, String type, PropertyList facets,
            String changeToken, String path, String state, String lockOwner,
            String lockCreated, String repository, String versionLabel,
            PropertyMap properties, PropertyMap contextParameters) {
        super(id);
        this.changeToken = changeToken;
        this.facets = facets;
        this.path = path;
        this.type = type;
        this.state = state;
        this.lockOwner = lockOwner;
        this.lockCreated = lockCreated;
        this.repository = repository;
        this.versionLabel = versionLabel;
        this.properties = properties == null ? new PropertyMap() : properties;
        this.contextParameters = contextParameters == null ? new PropertyMap()
                : contextParameters;
    }

    public String getRepository() {
        return repository;
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
        if (lockOwner != null && lockCreated != null) {
            return lockOwner + ":" + lockCreated;
        }
        return null;
    }
    
    public String getLockOwner() {
        return lockOwner;
    }
    
    public String getLockCreated() {
        return lockCreated;
    }
    
    public boolean isLocked() {
        return lockOwner != null;
    }
    
    public String getState() {
        return state;
    }

    public String getVersionLabel() {
        return versionLabel;
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

    public String getChangeToken() {
        return changeToken;
    }

    public PropertyList getFacets() {
        return facets;
    }

    public PropertyMap getContextParameters() {
        return contextParameters;
    }

}
