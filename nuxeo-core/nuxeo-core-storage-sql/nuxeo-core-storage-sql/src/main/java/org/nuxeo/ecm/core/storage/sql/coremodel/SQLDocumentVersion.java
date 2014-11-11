/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.EmptyDocumentIterator;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Node;

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
        Serializable versionSeriesId = getPropertyValue(Model.VERSION_VERSIONABLE_PROP);
        versionableNode = session.getNodeById(versionSeriesId);
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
            return !Boolean.TRUE.equals(versionableNode.getSimpleProperty(
                    Model.MAIN_CHECKED_IN_PROP).getValue());
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    @Override
    public boolean isMajorVersion() throws DocumentException {
        return Long.valueOf(0).equals(
                getPropertyValue(Model.MAIN_MINOR_VERSION_PROP));
    }

    @Override
    public boolean isLatestVersion() throws DocumentException {
        return Boolean.TRUE.equals(getPropertyValue(Model.VERSION_IS_LATEST_PROP));
    }

    @Override
    public boolean isLatestMajorVersion() throws DocumentException {
        return Boolean.TRUE.equals(getPropertyValue(Model.VERSION_IS_LATEST_MAJOR_PROP));
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
        Serializable versionSeriesId = getPropertyValue(Model.VERSION_VERSIONABLE_PROP);
        return session.idToString(versionSeriesId);
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

    /*
     * ----- property write overrides -----
     */

    @Override
    public void setPropertyValue(String name, Serializable value)
            throws DocumentException {
        if (isReadOnlyProperty(name)) {
            throw new VersionNotModifiableException(String.format(
                    "Cannot set property on a version: %s = %s", name, value));
        }
        // import
        super.setPropertyValue(name, value);
    }

    protected boolean isReadOnlyProperty(String name) {
        return isReadOnly() && !SQLSession.isVersionWritableProperty(name);
    }

    /*
     * ----- equals/hashcode -----
     */

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (other.getClass() == this.getClass()) {
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
