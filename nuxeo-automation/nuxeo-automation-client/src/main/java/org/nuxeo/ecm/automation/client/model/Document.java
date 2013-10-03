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
 * A document. Documents are as they are returned by the server. To modify
 * documents use operations. Use {@link #getProperties()} method to fetch the
 * document properties and {@link #getDirties()} to fetch dirty properties
 * updated.
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

    protected final String isCheckedOut;

    protected final PropertyMap properties;

    protected final transient PropertyMapSetter propertiesSetter;

    protected final PropertyMap contextParameters;

    protected final String changeToken;

    protected final PropertyList facets;

    @Deprecated
    /**
     * Deprecated now use with the constructor with versionLabel and isCheckedOut
     *
     */
    public Document(String id, String type, PropertyList facets,
            String changeToken, String path, String state, String lockOwner,
            String lockCreated, String repository, PropertyMap properties,
            PropertyMap contextParameters) {
        this(id, type, facets, changeToken, path, state, lockOwner,
                lockCreated, repository, null, null, properties,
                contextParameters);
    }

    @Deprecated
    /**
     * Deprecated now use with the constructor with isCheckedOut
     *
     */
    public Document(String id, String type, PropertyList facets,
            String changeToken, String path, String state, String lockOwner,
            String lockCreated, String repository, String versionLabel,
            PropertyMap properties, PropertyMap contextParameters) {
        this(id, type, facets, changeToken, path, state, lockOwner,
                lockCreated, repository, versionLabel, null, properties,
                contextParameters);
    }

    /**
     * Reserved to framework. Should be only called by client framework when
     * unmarshalling documents.
     *
     * @since 5.7.3
     */
    public Document(String id, String type, PropertyList facets,
            String changeToken, String path, String state, String lockOwner,
            String lockCreated, String repository, String versionLabel,
            String isCheckedOut, PropertyMap properties,
            PropertyMap contextParameters) {
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
        this.isCheckedOut = isCheckedOut;
        this.properties = properties == null ? new PropertyMap() : properties;
        this.contextParameters = contextParameters == null ? new PropertyMap()
                : contextParameters;
        propertiesSetter = new PropertyMapSetter(
                properties == null ? new PropertyMap() : properties);
    }

    /**
     * Minimal constructor for automation client Document. Could be instantiated
     * when creating a document and passing to the related automation operation.
     *
     * @since 5.7
     */
    public Document(String id, String type) {
        super(id);
        this.type = type;
        propertiesSetter = new PropertyMapSetter(new PropertyMap());
        changeToken = null;
        facets = null;
        path = null;
        state = null;
        lockOwner = null;
        lockCreated = null;
        repository = null;
        versionLabel = null;
        isCheckedOut = null;
        properties = new PropertyMap();
        contextParameters = new PropertyMap();
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

    public Boolean isCheckedOut() {
        return (isCheckedOut == null) ? null
                : Boolean.parseBoolean(isCheckedOut);
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
        propertiesSetter.set(key, defValue);
    }

    public void set(String key, Date defValue) {
        propertiesSetter.set(key, defValue);
    }

    public void set(String key, Long defValue) {
        propertiesSetter.set(key, defValue);
    }

    public void set(String key, Double defValue) {
        propertiesSetter.set(key, defValue);
    }

    /**
     * @since 5.7
     */
    public void set(String key, Boolean defValue) {
        propertiesSetter.set(key, defValue);
    }

    /**
     * @since 5.7
     */
    public void set(String key, PropertyMap defValue) {
        propertiesSetter.set(key, defValue);
    }

    /**
     * @since 5.7
     */
    public void set(String key, PropertyList defValue) {
        propertiesSetter.set(key, defValue);
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

    /**
     * This method fetch the dirty properties of the document (which have been
     * updated during the session)
     *
     * @since 5.7
     */
    public PropertyMap getDirties() {
        return propertiesSetter.getDirties();
    }

}
