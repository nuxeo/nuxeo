/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.model;

import java.util.Date;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * A document. Documents are as they are returned by the server. To modify documents use operations. Use
 * {@link #getProperties()} method to fetch the document properties and {@link #getDirties()} to fetch dirty properties
 * updated.
 * <p>
 * You need to create your own wrapper if you need to access the document properties in a multi-level way. This is a
 * flat representation of the document.
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

    /**
     * Reserved to framework. Should be only called by client framework when unmarshalling documents.
     *
     * @since 5.7.3
     */
    public Document(String id, String type, PropertyList facets, String changeToken, String path, String state,
            String lockOwner, String lockCreated, String repository, String versionLabel, String isCheckedOut,
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
        this.isCheckedOut = isCheckedOut;
        this.properties = properties == null ? new PropertyMap() : properties;
        this.contextParameters = contextParameters == null ? new PropertyMap() : contextParameters;
        propertiesSetter = new PropertyMapSetter(properties == null ? new PropertyMap() : properties);
    }

    /**
     * Minimal constructor for automation client Document. Could be instantiated when creating a document and passing to
     * the related automation operation.
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

    @JsonProperty("uid")
    public String getId() {
        return ref;
    }

    @JsonProperty("entity-type")
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
        return (isCheckedOut == null) ? null : Boolean.valueOf(isCheckedOut);
    }

    public Date getLastModified() {
        return properties.getDate("dc:modified");
    }

    public String getTitle() {
        return properties.getString("dc:title");
    }

    @JsonIgnore
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
     * This method fetch the dirty properties of the document (which have been updated during the session)
     *
     * @since 5.7
     */
    public PropertyMap getDirties() {
        return propertiesSetter.getDirties();
    }

}
