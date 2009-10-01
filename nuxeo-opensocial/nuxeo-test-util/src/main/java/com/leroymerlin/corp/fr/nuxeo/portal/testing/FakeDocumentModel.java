package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
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
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.schema.DocumentType;

public class FakeDocumentModel implements DocumentModel {

  private static final long serialVersionUID = 1L;

  private Map<String, Serializable> property = null;

  public FakeDocumentModel(String email, String listeLibelleRayon,
      String listeCodeRayon, String codeMagasin, String libelleMagasin,
      String mailUser, String mailHost, String region) {
    property = new HashMap<String, Serializable>();
    property.put("userlm:email", email);
    property.put("userlm:listeLibellesRayons", listeLibelleRayon);
    property.put("userlm:listeCodesRayons", listeCodeRayon);
    property.put("userlm:codeMagasin", codeMagasin);
    property.put("userlm:libelleMagasin", libelleMagasin);
    property.put("userlm:mailUser", mailUser);
    property.put("userlm:mailHost", mailHost);
    property.put("userlm:region", region);
  }

  public FakeDocumentModel(Map<String, Serializable> doc1Properties) {
    this.property = doc1Properties;
  }

  public FakeDocumentModel(String color, String layoutConfig){
    property = new HashMap<String, Serializable>();
    property.put("page:color", color);
    property.put("page:layoutConfig", layoutConfig);
  }

  public FakeDocumentModel(String email, String listeLibelleRayon,
      String listeCodeRayon, String codeMagasin, String libelleMagasin,
      String mailUser, String mailHost) {
    this(email, listeLibelleRayon, listeCodeRayon, codeMagasin, libelleMagasin,
        mailUser, mailHost, null);
  }

  @Override
  public DocumentModel clone() throws CloneNotSupportedException {
    return null;
  }

  public void copyContent(DocumentModel sourceDoc) {
  }

  public void copyContextData(DocumentModel otherDocument) {
  }

  public boolean followTransition(String transition) throws ClientException {
    return false;
  }

  public ACP getACP() {
    return null;
  }

  public <T> T getAdapter(Class<T> itf) {
    return null;
  }

  public <T> T getAdapter(Class<T> itf, boolean refreshCache) {
    return null;
  }

  public Collection<String> getAllowedStateTransitions() throws ClientException {
    return null;
  }

  public String getCacheKey() {
    return null;
  }

  public ScopedMap getContextData() {
    return null;
  }

  public Serializable getContextData(String key) {
    return null;
  }

  public Serializable getContextData(ScopeType scope, String key) {
    return null;
  }

  public String getCurrentLifeCycleState() throws ClientException {
    return null;
  }

  public DataModel getDataModel(String schema) {
    return null;
  }

  public DataModelMap getDataModels() {
    return null;
  }

  public Collection<DataModel> getDataModelsCollection() {
    return null;
  }

  public Set<String> getDeclaredFacets() {
    return null;
  }

  public String[] getDeclaredSchemas() {
    return null;
  }

  public DocumentType getDocumentType() {
    return null;
  }

  public long getFlags() {
    return 0;
  }

  public String getId() {
    return null;
  }

  public String getLifeCyclePolicy() throws ClientException {
    return null;
  }

  public String getLock() {
    return null;
  }

  public String getName() {
    return null;
  }

  public DocumentRef getParentRef() {
    return null;
  }

  public DocumentPart getPart(String schema) {
    return null;
  }

  public DocumentPart[] getParts() {
    return null;
  }

  public Path getPath() {
    return null;
  }

  public String getPathAsString() {
    return null;
  }

  public Map<String, Serializable> getPrefetch() {
    return null;
  }

  public Map<String, Object> getProperties(String schemaName) {
    return null;
  }

  public Property getProperty(String xpath) throws PropertyException {
    return null;
  }

  public Object getProperty(String schemaName, String name) {
    try {
      return this.getPropertyValue(schemaName+":"+name);
    } catch (PropertyException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

  public Serializable getPropertyValue(String xpath) throws PropertyException {
    Serializable value = property.get(xpath);
    return value;
  }

  public DocumentRef getRef() {
    return null;
  }

  public String getRepositoryName() {
    return null;
  }

  public String getSessionId() {
    return null;
  }

  public String getSourceId() {
    return null;
  }

  public <T extends Serializable> T getSystemProp(String systemProperty,
      Class<T> type) throws ClientException, DocumentException {
    return null;
  }

  public String getTitle() {
    return null;
  }

  public String getType() {
    return null;
  }

  public String getVersionLabel() {
    return null;
  }

  public boolean hasFacet(String facet) {
    return false;
  }

  public boolean hasSchema(String schema) {
    return false;
  }

  public boolean isDownloadable() {
    return false;
  }

  public boolean isFolder() {
    return false;
  }

  public boolean isLifeCycleLoaded() {
    return false;
  }

  public boolean isLocked() {
    return false;
  }

  public boolean isProxy() {
    return false;
  }

  public boolean isVersion() {
    return false;
  }

  public boolean isVersionable() {
    return false;
  }

  public void prefetchCurrentLifecycleState(String lifecycle) {
  }

  public void prefetchLifeCyclePolicy(String lifeCyclePolicy) {
  }

  public void prefetchProperty(String id, Object value) {
  }

  public void putContextData(String key, Serializable value) {
  }

  public void putContextData(ScopeType scope, String key, Serializable value) {
  }

  public void refresh() throws ClientException {
  }

  public void refresh(int refreshFlags, String[] schemas)
      throws ClientException {
  }

  public void reset() {
  }

  public void setACP(ACP acp, boolean overwrite) {
  }

  public void setLock(String key) throws ClientException {
  }

  public void setPathInfo(String parentPath, String name) {
  }

  public void setProperties(String schemaName, Map<String, Object> data) {
  }

  public void setProperty(String schemaName, String name, Object value) {
  }

  public void setPropertyValue(String xpath, Serializable value)
      throws PropertyException {
  }

  public void unlock() throws ClientException {
  }

  public CoreSession getCoreSession() {
	return null;
  }

}
