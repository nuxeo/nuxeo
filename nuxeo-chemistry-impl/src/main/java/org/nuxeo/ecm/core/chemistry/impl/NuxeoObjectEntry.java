/*
 * Copyright 2009 Nuxeo SA <http://nuxeo.com>
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
 * Authors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.chemistry.impl;

import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.chemistry.Document;
import org.apache.chemistry.Folder;
import org.apache.chemistry.ObjectEntry;
import org.apache.chemistry.Policy;
import org.apache.chemistry.Relationship;
import org.apache.chemistry.property.Property;
import org.apache.chemistry.property.PropertyDefinition;
import org.apache.chemistry.type.BaseType;
import org.apache.chemistry.type.ContentStreamPresence;
import org.apache.chemistry.type.Type;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

public class NuxeoObjectEntry implements ObjectEntry {

    private static final Log log = LogFactory.getLog(NuxeoObjectEntry.class);

    protected final DocumentModel doc;

    protected final NuxeoConnection connection;

    protected NuxeoObjectEntry(DocumentModel doc, NuxeoConnection connection) {
        this.doc = doc;
        this.connection = connection;
    }

    public Type getType() {
        return connection.repository.getType(doc.getType());
    }

    public Property getProperty(String name) {
        try {
            return NuxeoProperty.getProperty(doc, getType(), name,
                    connection.session);
        } catch (ClientException e) {
            throw new RuntimeException(e.toString(), e);
        }

    }

    public Map<String, Property> getProperties() {
        Map<String, Property> properties = new HashMap<String, Property>();
        for (PropertyDefinition pd : getType().getPropertyDefinitions()) {
            String name = pd.getName();
            properties.put(name, getProperty(name));
        }
        return properties;
    }

    public Serializable getValue(String name) {
        return getProperty(name).getValue();
    }

    public Collection<String> getAllowableActions() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<ObjectEntry> getRelationships() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /*
     * ----- link to Document/Folder/etc classes -----
     */

    public Document getDocument() {
        if (getType().getBaseType() != BaseType.DOCUMENT) {
            throw new RuntimeException("Not a document: " + getId());
        }
        return new NuxeoDocument(doc, connection);
    }

    public Folder getFolder() {
        if (getType().getBaseType() != BaseType.FOLDER) {
            throw new RuntimeException("Not a folder: " + getId());
        }
        return new NuxeoFolder(doc, connection);
    }

    public Relationship getRelationship() {
        if (getType().getBaseType() != BaseType.RELATIONSHIP) {
            throw new RuntimeException("Not a relationship: " + getId());
        }
        return new NuxeoRelationship(doc, connection);
    }

    public Policy getPolicy() {
        if (getType().getBaseType() != BaseType.POLICY) {
            throw new RuntimeException("Not a policy: " + getId());
        }
        return new NuxeoPolicy(doc, connection);
    }

    /*
     * ----- convenience methods -----
     */

    public String getString(String name) {
        return (String) getValue(name);
    }

    public String[] getStrings(String name) {
        return (String[]) getValue(name);
    }

    public BigDecimal getDecimal(String name) {
        return (BigDecimal) getValue(name);
    }

    public BigDecimal[] getDecimals(String name) {
        return (BigDecimal[]) getValue(name);
    }

    public Integer getInteger(String name) {
        return (Integer) getValue(name);
    }

    public Integer[] getIntegers(String name) {
        return (Integer[]) getValue(name);
    }

    public Boolean getBoolean(String name) {
        return (Boolean) getValue(name);
    }

    public Boolean[] getBooleans(String name) {
        return (Boolean[]) getValue(name);
    }

    public Calendar getDateTime(String name) {
        return (Calendar) getValue(name);
    }

    public Calendar[] getDateTimes(String name) {
        return (Calendar[]) getValue(name);
    }

    public URI getURI(String name) {
        return (URI) getValue(name);
    }

    public URI[] getURIs(String name) {
        return (URI[]) getValue(name);
    }

    public String getId(String name) {
        return (String) getValue(name);
    }

    public String[] getIds(String name) {
        return (String[]) getValue(name);
    }

    public String getXML(String name) {
        return (String) getValue(name);
    }

    public String[] getXMLs(String name) {
        return (String[]) getValue(name);
    }

    public String getHTML(String name) {
        return (String) getValue(name);
    }

    public String[] getHTMLs(String name) {
        return (String[]) getValue(name);
    }

    /*
     * ----- convenience methods for specific properties -----
     */

    public String getId() {
        return getString(Property.ID);
    }

    public URI getURI() {
        return getURI(Property.URI);
    }

    public String getTypeId() {
        return getId(Property.TYPE_ID);
    }

    public String getCreatedBy() {
        return getString(Property.CREATED_BY);
    }

    public Calendar getCreationDate() {
        return getDateTime(Property.CREATION_DATE);
    }

    public String getLastModifiedBy() {
        return getString(Property.LAST_MODIFIED_BY);
    }

    public Calendar getLastModificationDate() {
        return getDateTime(Property.LAST_MODIFICATION_DATE);
    }

    public String getChangeToken() {
        return getString(Property.CHANGE_TOKEN);
    }

    public String getName() {
        return getString(Property.NAME);
    }

    public boolean isImmutable() {
        Boolean b = getBoolean(Property.IS_IMMUTABLE);
        return b == null ? false : b.booleanValue();
    }

    public boolean isLatestVersion() {
        Boolean b = getBoolean(Property.IS_LATEST_VERSION);
        return b == null ? false : b.booleanValue();
    }

    public boolean isMajorVersion() {
        Boolean b = getBoolean(Property.IS_MAJOR_VERSION);
        return b == null ? false : b.booleanValue();
    }

    public boolean isLatestMajorVersion() {
        Boolean b = getBoolean(Property.IS_LATEST_MAJOR_VERSION);
        return b == null ? false : b.booleanValue();
    }

    public String getVersionLabel() {
        return getString(Property.VERSION_LABEL);
    }

    public String getVersionSeriesId() {
        return getId(Property.VERSION_SERIES_ID);
    }

    public boolean isVersionSeriesCheckedOut() {
        Boolean b = getBoolean(Property.IS_VERSION_SERIES_CHECKED_OUT);
        return b == null ? false : b.booleanValue();
    }

    public String getVersionSeriesCheckedOutBy() {
        return getString(Property.VERSION_SERIES_CHECKED_OUT_BY);
    }

    public String getVersionSeriesCheckedOutId() {
        return getId(Property.VERSION_SERIES_CHECKED_OUT_ID);
    }

    public String getCheckinComment() {
        return getString(Property.CHECKIN_COMMENT);
    }

    public boolean hasContentStream() {
        if (getType().getContentStreamAllowed() == ContentStreamPresence.NOT_ALLOWED) {
            return false;
        }
        if (!doc.hasSchema("file")) {
            return false;
        }
        try {
            return (Blob) doc.getProperty("file", "content") != null;
        } catch (ClientException e) {
            log.error("Could not check blob presence", e);
            return false;
        }
    }
}
