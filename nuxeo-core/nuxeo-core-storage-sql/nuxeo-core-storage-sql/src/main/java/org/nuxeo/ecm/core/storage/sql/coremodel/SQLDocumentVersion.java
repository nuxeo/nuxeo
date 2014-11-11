/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.EmptyDocumentIterator;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Node;

/**
 * @author Florent Guillaume
 */
public class SQLDocumentVersion extends SQLDocumentLive {

    private final Node versionableNode;

    public static class VersionNotModifiableException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public VersionNotModifiableException() {
            super();
        }

        public VersionNotModifiableException(String message) {
            super(message);
        }

    }

    protected SQLDocumentVersion(Node node, ComplexType type,
            SQLSession session, boolean readonly) throws DocumentException {
        super(node, type, session, readonly);
        versionableNode = session.getNodeById((Serializable) getProperty(
                Model.VERSION_VERSIONABLE_PROP).getValue());
    }

    /*
     * ----- version-specific overrides -----
     */

    @Override
    public boolean isVersion() {
        return true;
    }

    @Override
    public boolean isCheckedOut() throws DocumentException {
        return false;
    }

    @Override
    public boolean isVersionSeriesCheckedOut() throws DocumentException {
        if (versionableNode == null) {
            return false;
        }
        try {
            Boolean b = (Boolean) versionableNode.getSimpleProperty(
                    Model.MAIN_CHECKED_IN_PROP).getValue();
            return b == null ? true : !b.booleanValue();
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    @Override
    public boolean isMajorVersion() throws DocumentException {
        return Long.valueOf(0).equals(
                getProperty(Model.MAIN_MINOR_VERSION_PROP).getValue());
    }

    @Override
    public boolean isLatestVersion() throws DocumentException {
        return getBoolean(Model.VERSION_IS_LATEST_PROP);
    }

    @Override
    public boolean isLatestMajorVersion() throws DocumentException {
        return getBoolean(Model.VERSION_IS_LATEST_MAJOR_PROP);
    }

    @Override
    public Document getWorkingCopy() throws DocumentException {
        if (versionableNode == null) {
            return null;
        }
        return session.getDocumentByUUID(versionableNode.getId().toString());
    }

    @Override
    public Document getBaseVersion() throws DocumentException {
        return null;
    }

    @Override
    public String getVersionSeriesId() throws DocumentException {
        return getString(Model.VERSION_VERSIONABLE_PROP);
    }

    @Override
    public Document getSourceDocument() throws DocumentException {
        return getWorkingCopy();
    }

    @Override
    public String getPath() throws DocumentException {
        if (versionableNode == null) {
            return null; // TODO return what? error?
        }
        return session.getPath(versionableNode);
    }

    @Override
    public Document getParent() throws DocumentException {
        if (versionableNode == null) {
            return null;
        }
        return session.getParent(versionableNode);
    }

    // protected Property getACLProperty() not overriden, no ACL anyway

    /*
     * ----- folder overrides -----
     */

    @Override
    public boolean isFolder() {
        return false;
    }

    @Override
    public void removeChild(String name) throws DocumentException {
        throw new VersionNotModifiableException();
    }

    @Override
    public void orderBefore(String src, String dest) throws DocumentException {
        throw new VersionNotModifiableException();
    }

    @Override
    public Document addChild(String name, String typeName)
            throws DocumentException {
        throw new VersionNotModifiableException();
    }

    @Override
    public Document getChild(String name) throws DocumentException {
        throw new NoSuchDocumentException(name);
    }

    @Override
    public Iterator<Document> getChildren() throws DocumentException {
        return EmptyDocumentIterator.INSTANCE;
    }

    @Override
    public List<String> getChildrenIds() throws DocumentException {
        return Collections.emptyList();
    }

    @Override
    public boolean hasChild(String name) throws DocumentException {
        return false;
    }

    @Override
    public boolean hasChildren() throws DocumentException {
        return false;
    }

    /*
     * ----- versioning overrides -----
     */

    @Override
    public Document checkIn(String label, String description) {
        throw new VersionNotModifiableException();
    }

    @Override
    public void checkOut() {
        throw new VersionNotModifiableException();
    }

    @Override
    public void restore(Document version) {
        throw new VersionNotModifiableException();
    }

    @Override
    public Document getVersion(String label) {
        return null;
    }

    @Override
    public List<Document> getVersions() {
        return Collections.emptyList();
    }

    @Override
    public boolean hasVersions() {
        return false;
    }

    @Override
    public Document getLastVersion() {
        return null;
    }

    /*
     * ----- property write overrides -----
     */

    @Override
    public void importFlatMap(Map<String, Object> map) {
        throw new VersionNotModifiableException();
    }

    @Override
    public void importMap(Map<String, Map<String, Object>> map) {
        throw new VersionNotModifiableException();
    }

    @Override
    public void setPropertyValue(String name, Object value)
            throws DocumentException {
        if (readonly
                && !SQLSimpleProperty.VERSION_WRITABLE_PROPS.contains(name)) {
            throw new VersionNotModifiableException(String.format(
                    "Cannot set property on a version: %s = %s", name, value));
        }
        // import
        super.setPropertyValue(name, value);
    }

    @Override
    public void setString(String name, String value) throws DocumentException {
        if (readonly
                && !SQLSimpleProperty.VERSION_WRITABLE_PROPS.contains(name)) {
            throw new VersionNotModifiableException();
        }
        super.setString(name, value);
    }

    @Override
    public void setBoolean(String name, boolean value) throws DocumentException {
        if (readonly
                && !SQLSimpleProperty.VERSION_WRITABLE_PROPS.contains(name)) {
            throw new VersionNotModifiableException();
        }
        // import
        super.setBoolean(name, value);
    }

    @Override
    public void setLong(String name, long value) {
        throw new VersionNotModifiableException();
    }

    @Override
    public void setDouble(String name, double value) {
        throw new VersionNotModifiableException();
    }

    @Override
    public void setDate(String name, Calendar value) {
        throw new VersionNotModifiableException();
    }

    @Override
    public void setContent(String name, Blob value) {
        throw new VersionNotModifiableException();
    }

    @Override
    public void removeProperty(String name) {
        throw new VersionNotModifiableException();
    }

    /*
     * ----- equals/hashcode -----
     */

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof SQLDocumentVersion) {
            return equals((SQLDocumentVersion) other);
        }
        return false;
    }

    private boolean equals(SQLDocumentVersion other) {
        return getNode().equals(other.getNode());
    }

    @Override
    public int hashCode() {
        return getNode().hashCode();
    }

}
