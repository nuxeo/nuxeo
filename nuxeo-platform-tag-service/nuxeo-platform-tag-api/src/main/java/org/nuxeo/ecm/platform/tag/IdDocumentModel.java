package org.nuxeo.ecm.platform.tag;

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
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.schema.DocumentType;

public class IdDocumentModel implements DocumentModel {

    private static final long serialVersionUID = 1L;

    protected IdRef ref;

    protected String id;

    public IdDocumentModel(String id) {
        ref = new IdRef(id);
        this.id = id;
    }

    public void copyContent(DocumentModel sourceDoc) throws ClientException {
        // TODO Auto-generated method stub
    }

    public void copyContextData(DocumentModel otherDocument) {
        // TODO Auto-generated method stub
    }

    public boolean followTransition(String transition) throws ClientException {
        // TODO Auto-generated method stub
        return false;
    }

    public ACP getACP() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public <T> T getAdapter(Class<T> itf) {
        // TODO Auto-generated method stub
        return null;
    }

    public <T> T getAdapter(Class<T> itf, boolean refreshCache) {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<String> getAllowedStateTransitions()
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getCacheKey() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public ScopedMap getContextData() {
        // TODO Auto-generated method stub
        return null;
    }

    public Serializable getContextData(String key) {
        // TODO Auto-generated method stub
        return null;
    }

    public Serializable getContextData(ScopeType scope, String key) {
        // TODO Auto-generated method stub
        return null;
    }

    public CoreSession getCoreSession() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getCurrentLifeCycleState() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DataModel getDataModel(String schema) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DataModelMap getDataModels() {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<DataModel> getDataModelsCollection() {
        // TODO Auto-generated method stub
        return null;
    }

    public Set<String> getDeclaredFacets() {
        // TODO Auto-generated method stub
        return null;
    }

    public String[] getDeclaredSchemas() {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentType getDocumentType() {
        // TODO Auto-generated method stub
        return null;
    }

    public long getFlags() {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getId() {
        return id;
    }

    public String getLifeCyclePolicy() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getLock() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentRef getParentRef() {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentPart getPart(String schema) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentPart[] getParts() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public Path getPath() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getPathAsString() {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<String, Serializable> getPrefetch() {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<String, Object> getProperties(String schemaName)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public Property getProperty(String xpath) throws PropertyException,
            ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getProperty(String schemaName, String name)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public Serializable getPropertyValue(String xpath)
            throws PropertyException, ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentRef getRef() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getRepositoryName() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getSessionId() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getSourceId() {
        // TODO Auto-generated method stub
        return null;
    }

    public <T extends Serializable> T getSystemProp(String systemProperty,
            Class<T> type) throws ClientException, DocumentException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getTitle() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getType() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getVersionLabel() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean hasFacet(String facet) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean hasSchema(String schema) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isDownloadable() throws ClientException {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isFolder() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isLifeCycleLoaded() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isLocked() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isProxy() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isImmutable() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isVersion() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isVersionable() {
        // TODO Auto-generated method stub
        return false;
    }

    public void prefetchCurrentLifecycleState(String lifecycle) {
        // TODO Auto-generated method stub
    }

    public void prefetchLifeCyclePolicy(String lifeCyclePolicy) {
        // TODO Auto-generated method stub
    }

    public void prefetchProperty(String id, Object value) {
        // TODO Auto-generated method stub
    }

    public void putContextData(String key, Serializable value) {
        // TODO Auto-generated method stub
    }

    public void putContextData(ScopeType scope, String key, Serializable value) {
        // TODO Auto-generated method stub
    }

    public void refresh() throws ClientException {
        // TODO Auto-generated method stub
    }

    public void refresh(int refreshFlags, String[] schemas)
            throws ClientException {
        // TODO Auto-generated method stub
    }

    public void reset() {
        // TODO Auto-generated method stub
    }

    public void setACP(ACP acp, boolean overwrite) throws ClientException {
        // TODO Auto-generated method stub
    }

    public void setLock(String key) throws ClientException {
        // TODO Auto-generated method stub
    }

    public void setPathInfo(String parentPath, String name) {
        // TODO Auto-generated method stub
    }

    public void setProperties(String schemaName, Map<String, Object> data)
            throws ClientException {
        // TODO Auto-generated method stub
    }

    public void setProperty(String schemaName, String name, Object value)
            throws ClientException {
        // TODO Auto-generated method stub
    }

    public void setPropertyValue(String xpath, Serializable value)
            throws PropertyException, ClientException {
        // TODO Auto-generated method stub
    }

    public void unlock() throws ClientException {
        // TODO Auto-generated method stub
    }

    @Override
    public DocumentModel clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

}
