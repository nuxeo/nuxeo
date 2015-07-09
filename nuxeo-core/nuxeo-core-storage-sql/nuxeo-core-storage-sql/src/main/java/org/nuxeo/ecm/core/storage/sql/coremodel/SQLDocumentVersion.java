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
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.schema.types.ComplexType;
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

    protected SQLDocumentVersion(Node node, ComplexType type, SQLSession session, boolean readonly) {
        super(node, type, session, readonly);
        Serializable versionSeriesId = getPropertyValue(Model.VERSION_VERSIONABLE_PROP);
        if (versionSeriesId == null) {
            throw new DocumentNotFoundException("Version was removed: " + node.getId());
        }
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
    public boolean isCheckedOut() {
        return false;
    }

    @Override
    public boolean isVersionSeriesCheckedOut() {
        if (versionableNode == null) {
            return false;
        }
        return !Boolean.TRUE.equals(versionableNode.getSimpleProperty(Model.MAIN_CHECKED_IN_PROP).getValue());
    }

    @Override
    public boolean isMajorVersion() {
        return Long.valueOf(0).equals(getPropertyValue(Model.MAIN_MINOR_VERSION_PROP));
    }

    @Override
    public boolean isLatestVersion() {
        return Boolean.TRUE.equals(getPropertyValue(Model.VERSION_IS_LATEST_PROP));
    }

    @Override
    public boolean isLatestMajorVersion() {
        return Boolean.TRUE.equals(getPropertyValue(Model.VERSION_IS_LATEST_MAJOR_PROP));
    }

    @Override
    public Document getWorkingCopy() {
        if (versionableNode == null) {
            return null;
        }
        return session.getDocumentByUUID(versionableNode.getId().toString());
    }

    @Override
    public Document getBaseVersion() {
        return null;
    }

    @Override
    public String getVersionSeriesId() {
        Serializable versionSeriesId = getPropertyValue(Model.VERSION_VERSIONABLE_PROP);
        return session.idToString(versionSeriesId);
    }

    @Override
    public Document getSourceDocument() {
        return getWorkingCopy();
    }

    @Override
    public String getPath() {
        if (versionableNode == null) {
            return null; // TODO return what? error?
        }
        return session.getPath(versionableNode);
    }

    @Override
    public Document getParent() {
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
    public void orderBefore(String src, String dest) {
        throw new VersionNotModifiableException();
    }

    @Override
    public Document addChild(String name, String typeName) {
        throw new VersionNotModifiableException();
    }

    @Override
    public Document getChild(String name) {
        throw new DocumentNotFoundException(name);
    }

    @Override
    public List<Document> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getChildrenIds() {
        return Collections.emptyList();
    }

    @Override
    public boolean hasChild(String name) {
        return false;
    }

    @Override
    public boolean hasChildren() {
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
    public void setPropertyValue(String name, Serializable value) {
        if (isReadOnlyProperty(name)) {
            throw new VersionNotModifiableException(String.format("Cannot set property on a version: %s = %s", name,
                    value));
        }
        // import
        super.setPropertyValue(name, value);
    }

    protected boolean isReadOnlyProperty(String name) {
        return isReadOnly() && !isVersionWritableProperty(name);
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
