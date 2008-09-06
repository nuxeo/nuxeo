/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.cache.data;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DataModelMap;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.platform.cache.CacheRuntimeException;
import org.nuxeo.ecm.platform.cache.server.bd.CacheableDocumentManager;

/**
 * A DocumentModelGhost is a DocumentModel implementation that will lazy load
 * data from a cached DocumentModel.
 * <p>
 * Plain data is kept inside this object. Complex data is retrieved through
 * business delegate like accessor methods in this class.
 * <p>
 * This class objects will be added with a reference to the DocumentManager when
 * they arrive in the "CacheClient".
 *
 * @author DM
 *
 */
public class DocumentModelGhost implements DocumentModel {

    private static final long serialVersionUID = 6891166715601027126L;

    // private static final Logger log =
    // Logger.getLogger(DocumentModelGhost.class);

    protected final String sid;

    protected final DocumentRef ref;

    protected final String type;

    // protected String[] declaredSchemas;

    protected final String id;

    protected final Path path;

    //
    protected final DocumentRef parentRef;

    // cache the reference to DocumentModel object available in the cache
    private DocumentModel documentModel;

    // PROPERTY IN CACHE ONLY:
    // protected Set<String> declaredFacets;
    // protected Map<String, DataModel> dataModels;
    // protected DocumentRef parentRef;

    // private DocumentManager documentManager;

    /**
     * Constructor for DocumentModelGhost. Makes a shallow copy of the original
     * DocumentModel, this shallow is what is bundled within the direct response
     * from server.
     *
     */
    public DocumentModelGhost(DocumentRef ref, String sid, String type,
            String[] declaredSchemas, String id, Path path,
            DocumentRef parentRef) {
        this.ref = ref;
        this.sid = sid;
        this.type = type;
        // this.declaredSchemas = declaredSchemas;
        this.id = id;
        this.path = path;
        this.parentRef = parentRef;
    }

    protected final CoreSession getClient() {
        if (sid == null) {
            throw new UnsupportedOperationException(
                    "Cannot load data models for client defined models");
        }
        return CoreInstance.getInstance().getSession(sid);
    }

    /*
     * This should be used in Client Cache to set the reference to
     * DocumentManager stub (=deployment implementation on client).
     *
     * @param documentManager
     */
    /*
     * public void setDocumentManager(DocumentManager documentManager) {
     * this.documentManager = documentManager; }
     */

    private DocumentModel getDM() {
        if (documentModel != null) {
            return documentModel;
        }
        // should obtain a reference to a real DocumentModelImpl
        // either from cache (if exists) or from server
        try {
            CoreSession coreSession = getClient();
            CacheableDocumentManager dm = (CacheableDocumentManager) coreSession;
            // documentModel = getClient().getDocument(ref);

            documentModel = getClient().getDocument(ref);
            if (documentModel instanceof DocumentModelGhost) {
                documentModel = dm.getDocumentImpl(ref);
            }
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new CacheRuntimeException(
                    "Error loading DocumentModel from Ghost", e);
        } catch (ClientException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new CacheRuntimeException(
                    "Error loading DocumentModel from Ghost", e);
        }

        return documentModel;
    }

    // ===============================================
    // START plain data accessors
    //
    // field: id
    public String getId() {
        return id;
    }

    /**
     * Implementation copied from {@link DocumentModelImpl#getName()}.
     */
    public String getName() {
        return path.lastSegment();
    }

    public String getTitle() {
        return getDM().getTitle();
    }

    /**
     * Implementation copied from {@link DocumentModelImpl#getParentRef()}.
     */
    public DocumentRef getParentRef() {
        return parentRef;
    }

    public DocumentRef getRef() {
        return ref;
    }

    public Path getPath() {
        return path;
    }

    public String getPathAsString() {
        return path.toString();
    }

    public String getSessionId() {
        return sid;
    }

    public String getType() {
        return type;
    }

    public DocumentType getDocumentType() {
        CoreInstance ci = CoreInstance.getInstance();
        DocumentType docType = ci.getCachedDocumentType(type);
        if (docType == null) {
            docType = getClient().getDocumentType(type);
            ci.cacheDocumentType(docType);
        }
        return docType;
    }

    //
    // END plain data accessors
    // ================================================

    public Map<String, Object> getProperties(String schemaName) {
        return getDM().getProperties(schemaName);
    }

    public Object getProperty(String schemaName, String name) {
        return getDM().getProperty(schemaName, name);
    }

    public boolean hasFacet(String facet) {
        return getDM().hasFacet(facet);
    }

    public boolean hasSchema(String schema) {
        return getDM().hasSchema(schema);
    }

    public boolean isFolder() {
        return getDM().isFolder();
    }

    public void setACP(ACP acp, boolean overwrite) {
        getDM().setACP(acp, overwrite);
    }

    public void setProperties(String schemaName, Map<String, Object> data) {
        getDM().setProperties(schemaName, data);
    }

    public void setProperty(String schemaName, String name, Object value) {
        getDM().setProperty(schemaName, name, value);
    }

    public void setPathInfo(String parentPath, String name) {
        getDM().setPathInfo(parentPath, name);
    }

    public ACP getACP() {
        return getDM().getACP();
    }

    public String getLock() {
        return getDM().getLock();
    }

    public boolean isLocked() {
        return getDM().isLocked();
    }

    public void setLock(String key) throws ClientException {
        getDM().setLock(key);
    }

    public void unlock() throws ClientException {
        getDM().unlock();
    }

    public DataModel getDataModel(String schema) {
        return getDM().getDataModel(schema);
    }

    public DataModelMap getDataModels() {
        return getDM().getDataModels();
    }

    public Collection<DataModel> getDataModelsCollection() {
        return getDM().getDataModelsCollection();
    }

    public Set<String> getDeclaredFacets() {
        // TODO Auto-generated method stub
        return getDM().getDeclaredFacets();
    }

    public String[] getDeclaredSchemas() {
        return getDM().getDeclaredSchemas();
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();

        // DO NOT call methods here, just rely on fields
        // calling methods will try to obtain a real doc
        // and we can fall into an infinite loop ...

        buf.append("DocumentModelGhost {");
        // buf.append(" -title: ");
        // buf.append(getProperty("dublincore", "title"));
        buf.append(", sessionId: ");
        buf.append(sid);
        buf.append(", doc id: ");
        buf.append(id);
        buf.append(", name: ");
        buf.append(path.lastSegment());
        buf.append(", path: ");
        buf.append(path);
        buf.append(", ref: ");
        buf.append(ref);
        buf.append(", parent ref: ");
        buf.append(parentRef);
        // buf.append(", data models: ");
        // buf.append(getDataModels());
        // buf.append(", declaredFacets: ");
        // buf.append(getDeclaredFacets());
        // buf.append(", declaredSchemas: ");
        // buf.append(getDeclaredSchemas());
        buf.append('}');

        return buf.toString();
    }

    public boolean isVersionable() {
        return getDM().isVersionable();
    }

    public boolean isDownloadable() {
        return getDM().isDownloadable();
    }

    public <T> T getAdapter(Class<T> itf) {
        return getDM().getAdapter(itf);
    }

    public boolean followTransition(String transition) throws ClientException {
        return getDM().followTransition(transition);
    }

    public Collection<String> getAllowedStateTransitions()
            throws ClientException {
        return getDM().getAllowedStateTransitions();
    }

    public String getCurrentLifeCycleState() throws ClientException {
        return getDM().getCurrentLifeCycleState();
    }

    public String getLifeCyclePolicy() throws ClientException {
        return getDM().getLifeCyclePolicy();
    }

    public boolean isVersion() {
        return getDM().isVersion();
    }

    public ScopedMap getContextData() {
        return getDM().getContextData();
    }

    public Serializable getContextData(ScopeType scope, String key) {
        return getDM().getContextData(scope, key);
    }

    public void putContextData(ScopeType scope, String key, Serializable value) {
        getDM().putContextData(scope, key, value);
    }

    public Serializable getContextData(String key) {
        return getDM().getContextData(key);
    }

    public void putContextData(String key, Serializable value) {
        getDM().putContextData(key, value);
    }

    public void copyContextData(DocumentModel otherDocument) {
        getDM().copyContextData(otherDocument);
    }

    /**
     * @see DocumentModel#copyContent(DocumentModel)
     */
    public void copyContent(DocumentModel sourceDoc) {
        getDM().copyContent(sourceDoc);
    }

    /**
     * @see DocumentModel#getCacheKey()
     */
    public String getCacheKey() {
        return getDM().getCacheKey();
    }

    /**
     * @see DocumentModel#getRepositoryName()
     */
    public String getRepositoryName() {
        return getDM().getRepositoryName();
    }

    /**
     * @see DocumentModel#getSourceId()
     */
    public String getSourceId() {
        return getDM().getSourceId();
    }

    /**
     * @see DocumentModel#getVersionLabel()
     */
    public String getVersionLabel() {
        return getDM().getVersionLabel();
    }

    /**
     * @see DocumentModel#isProxy()
     */
    public boolean isProxy() {
        return getDM().isProxy();
    }

    public <T> T getAdapter(Class<T> itf, boolean refreshCache) {
        return getDM().getAdapter(itf, refreshCache);
    }

    public Map<String, Serializable> getPrefetch() {
        return getDM().getPrefetch();
    }

    public void prefetchProperty(String id, Object value) {
        getDM().prefetchProperty(id, value);
    }

    public <T extends Serializable> T getSystemProp(String systemProperty,
            Class<T> type) throws DocumentException, ClientException {
        return getDM().getSystemProp(systemProperty, type);
    }

    public void prefetchCurrentLifecycleState(String lifecycle) {
        getDM().prefetchCurrentLifecycleState(lifecycle);
    }

    public void prefetchLifeCyclePolicy(String lifeCyclePolicy) {
        getDM().prefetchLifeCyclePolicy(lifeCyclePolicy);
    }

    public boolean isLifeCycleLoaded() {
        return getDM().isLifeCycleLoaded();
    }

    public DocumentPart getPart(String schema) {
        return getDM().getPart(schema);
    }

    public DocumentPart[] getParts() {
        return getDM().getParts();
    }

    public Property getProperty(String xpath) throws PropertyException {
        return getDM().getProperty(xpath);
    }

    public Serializable getPropertyValue(String xpath) throws PropertyException {
        return getDM().getPropertyValue(xpath);
    }

    public void setPropertyValue(String xpath, Serializable value)
            throws PropertyException {
        getDM().setPropertyValue(xpath, value);
    }

    public long getFlags() {
        return getDM().getFlags();
    }

    @Override
    public DocumentModel clone() throws CloneNotSupportedException {
        return getDM().clone();
    }

    public void reset() {
        getDM().reset();
    }

    public void refresh() throws ClientException {
        getDM().refresh();
    }

    public void refresh(int refreshFlags, String[] schemas)
            throws ClientException {
        getDM().refresh(refreshFlags, schemas);
    }

}
