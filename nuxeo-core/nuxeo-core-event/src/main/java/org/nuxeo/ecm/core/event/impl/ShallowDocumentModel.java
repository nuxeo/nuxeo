/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.event.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DataModelMap;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.schema.DocumentType;

/**
 *
 * Light weight {@link DocumentModel} implementation Only holds
 * {@link DocumentRef}, RepositoryName, name and path. Used to reduce memory
 * footprint of {@link Event} stacked in {@link EventBundle}
 *
 * @author Thierry Delprat
 *
 */
public class ShallowDocumentModel implements DocumentModel {

    private static final long serialVersionUID = 1L;

    private final String id;

    private final String repoName;

    private final String name;

    private final Path path;

    private final String type;

    private final boolean isFolder;

    public ShallowDocumentModel(DocumentModel doc) {
        id = doc.getId();
        repoName = doc.getRepositoryName();
        name = doc.getName();
        path = doc.getPath();
        type = doc.getType();
        isFolder = doc.isFolder();
    }

    public String getId() {
        return id;
    }

    public DocumentRef getRef() {
        return id == null ? null : new IdRef(id);
    }

    public String getRepositoryName() {
        return repoName;
    }

    public String getName() {
        return name;
    }

    public Path getPath() {
        return path;
    }

    public String getPathAsString() {
        if (path != null) {
            return path.toString();
        }
        return null;
    }

    public DocumentRef getParentRef() {
        if (path != null) {
            return new PathRef(path.removeLastSegments(1).toString());
        }
        return null;
    }

    public String getType() {
        return type;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public void copyContent(DocumentModel sourceDoc) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void copyContextData(DocumentModel otherDocument) {
        throw new UnsupportedOperationException();
    }

    public boolean followTransition(String transition) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public ACP getACP() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public <T> T getAdapter(Class<T> itf) {
        throw new UnsupportedOperationException();
    }

    public <T> T getAdapter(Class<T> itf, boolean refreshCache) {
        throw new UnsupportedOperationException();
    }

    public Collection<String> getAllowedStateTransitions()
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    public String getCacheKey() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public ScopedMap getContextData() {
        throw new UnsupportedOperationException();
    }

    public Serializable getContextData(ScopeType scope, String key) {
        throw new UnsupportedOperationException();
    }

    public CoreSession getCoreSession() {
        throw new UnsupportedOperationException();
    }

    public String getCurrentLifeCycleState() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public DataModel getDataModel(String schema) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public DataModelMap getDataModels() {
        throw new UnsupportedOperationException();
    }

    public Collection<DataModel> getDataModelsCollection() {
        throw new UnsupportedOperationException();
    }

    public Set<String> getDeclaredFacets() {
        throw new UnsupportedOperationException();
    }

    public String[] getDeclaredSchemas() {
        throw new UnsupportedOperationException();
    }

    public DocumentType getDocumentType() {
        throw new UnsupportedOperationException();
    }

    public long getFlags() {
        throw new UnsupportedOperationException();
    }

    public String getLifeCyclePolicy() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public String getLock() {
        throw new UnsupportedOperationException();
    }

    public DocumentPart getPart(String schema) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public DocumentPart[] getParts() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public Map<String, Serializable> getPrefetch() {
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getProperties(String schemaName)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    public Property getProperty(String xpath) throws PropertyException,
            ClientException {
        throw new UnsupportedOperationException();
    }

    public Object getProperty(String schemaName, String name)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    public Serializable getPropertyValue(String xpath)
            throws PropertyException, ClientException {
        throw new UnsupportedOperationException();
    }

    public String getSessionId() {
        throw new UnsupportedOperationException();
    }

    public String getSourceId() {
        throw new UnsupportedOperationException();
    }

    public <T extends Serializable> T getSystemProp(String systemProperty,
            Class<T> type) throws ClientException, DocumentException {
        throw new UnsupportedOperationException();
    }

    public String getTitle() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public String getVersionLabel() {
        throw new UnsupportedOperationException();
    }

    public boolean hasFacet(String facet) {
        throw new UnsupportedOperationException();
    }

    public boolean hasSchema(String schema) {
        throw new UnsupportedOperationException();
    }

    public boolean isDownloadable() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public boolean isLifeCycleLoaded() {
        throw new UnsupportedOperationException();
    }

    public boolean isLocked() {
        throw new UnsupportedOperationException();
    }

    public boolean isProxy() {
        throw new UnsupportedOperationException();
    }

    public boolean isVersion() {
        throw new UnsupportedOperationException();
    }

    public boolean isVersionable() {
        throw new UnsupportedOperationException();
    }

    public void prefetchCurrentLifecycleState(String lifecycle) {
        throw new UnsupportedOperationException();
    }

    public void prefetchLifeCyclePolicy(String lifeCyclePolicy) {
        throw new UnsupportedOperationException();
    }

    public void prefetchProperty(String id, Object value) {
        throw new UnsupportedOperationException();
    }

    public void putContextData(String key, Serializable value) {
        throw new UnsupportedOperationException();
    }

    public void putContextData(ScopeType scope, String key, Serializable value) {
        throw new UnsupportedOperationException();
    }

    public void refresh() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void refresh(int refreshFlags, String[] schemas)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void reset() {
        throw new UnsupportedOperationException();
    }

    public void setACP(ACP acp, boolean overwrite) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void setLock(String key) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void setPathInfo(String parentPath, String name) {
        throw new UnsupportedOperationException();
    }

    public void setProperties(String schemaName, Map<String, Object> data)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void setProperty(String schemaName, String name, Object value)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void setPropertyValue(String xpath, Serializable value) {
        throw new UnsupportedOperationException();
    }

    public void unlock() throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public Serializable getContextData(String key) {
        throw new UnsupportedOperationException();
    }

}
