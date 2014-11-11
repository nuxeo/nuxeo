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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.ecm.core.versioning.DocumentVersion;
import org.nuxeo.ecm.core.versioning.DocumentVersionIterator;

/**
 * @author Florent Guillaume
 */
public class SQLDocumentVersion extends SQLDocument implements DocumentVersion {

    private final Node versionableNode;

    protected SQLDocumentVersion(Node node, ComplexType type, SQLSession session)
            throws DocumentException {
        super(node, type, session, true);
        versionableNode = session.getNodeById((Serializable) getProperty(
                Model.VERSION_VERSIONABLE_PROP).getValue());
    }

    /*
     * ----- DocumentVersion -----
     */

    public String getLabel() throws DocumentException {
        return getString(Model.VERSION_LABEL_PROP);
    }

    public String getDescription() throws DocumentException {
        return getString(Model.VERSION_DESCRIPTION_PROP);
    }

    public Calendar getCreated() throws DocumentException {
        return getDate(Model.VERSION_CREATED_PROP);
    }

    // API unused
    public DocumentVersion[] getPredecessors() {
        throw new UnsupportedOperationException();
    }

    // API unused
    public DocumentVersion[] getSuccessors() {
        throw new UnsupportedOperationException();
    }

    /*
     * ----- version-specific overrides -----
     */

    @Override
    public boolean isVersion() {
        return true;
    }

    @Override
    public Document getSourceDocument() throws DocumentException {
        if (versionableNode == null) {
            return null;
        }
        return session.getDocumentByUUID(versionableNode.getId().toString());
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
    public void removeChild(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void orderBefore(String src, String dest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Document addChild(String name, String typeName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Document getChild(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Document> getChildren() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasChild(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasChildren() {
        throw new UnsupportedOperationException();
    }

    /*
     * ----- versioning overrides -----
     */

    @Override
    public void checkIn(String label) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkIn(String label, String description) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkOut() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCheckedOut() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restore(String label) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Document getVersion(String label) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentVersionIterator getVersions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasVersions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentVersion getLastVersion() {
        throw new UnsupportedOperationException();
    }

    /*
     * ----- property write overrides -----
     */

    @Override
    public void importFlatMap(Map<String, Object> map) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void importMap(Map<String, Map<String, Object>> map) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPropertyValue(String name, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setString(String name, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBoolean(String name, boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLong(String name, long value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDouble(String name, double value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDate(String name, Calendar value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContent(String name, Blob value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeProperty(String name) {
        throw new UnsupportedOperationException();
    }

    /*
     * ----- lifecycle overrides -----
     */

    @Override
    public Collection<String> getAllowedStateTransitions() {
        return Collections.emptyList();
    }

    @Override
    public boolean followTransition(String transition)
            throws LifeCycleException {
        throw new LifeCycleException("Cannot follow lifecycle transitions");
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
        return getHierarchyNode().getId() == other.getHierarchyNode().getId();
    }

    @Override
    public int hashCode() {
        return getHierarchyNode().getId().hashCode();
    }

}
