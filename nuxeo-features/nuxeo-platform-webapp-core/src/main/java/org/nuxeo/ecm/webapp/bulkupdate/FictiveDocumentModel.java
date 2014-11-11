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

package org.nuxeo.ecm.webapp.bulkupdate;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DataModelMap;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.DataModelMapImpl;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.impl.DefaultPropertyFactory;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.schema.DocumentType;

/**
 * A DocumentModel that can have any schema and is not made persistent by
 * itself. A mockup to keep arbitrary schema data.
 *
 * @author DM
 */
// TODO move to a common availability package
public class FictiveDocumentModel implements DocumentModel, Serializable {

    private static final Log log = LogFactory.getLog(FictiveDocumentModel.class);

    private static final long serialVersionUID = 9196449956494350461L;

    private final DataModelMap dataModels = new DataModelMapImpl();

    private String type;

    public boolean followTransition(String transition) throws ClientException {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public ACP getACP() {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public <T> T getAdapter(Class<T> itf) {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public Collection<String> getAllowedStateTransitions()
            throws ClientException {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public String getCurrentLifeCycleState() throws ClientException {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public DataModel getDataModel(String schema) {
        return getDataModel(schema, true);
    }

    /**
     *
     * @param schema
     * @param create if <code>true</code> creates the dataModel corresponding
     *            to the given schema if doesn't exist
     * @return
     */
    public DataModel getDataModel(String schema, boolean create) {
        // if not available, create
        DataModel dataModel = dataModels.get(schema);
        if (dataModel == null && create) {
            log.warn("schema was not registered before: " + schema);
            dataModel = new FictiveDataModel();
            dataModels.put(schema, dataModel);
        }
        return dataModel;
    }

    public DataModelMap getDataModels() {
        return dataModels;
    }

    public Collection<DataModel> getDataModelsCollection() {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public Set<String> getDeclaredFacets() {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public String[] getDeclaredSchemas() {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public String getId() {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public String getLifeCyclePolicy() throws ClientException {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public String getLock() {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public String getName() {
        return "fictiveDocumentModel";
    }

    public DocumentRef getParentRef() {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public Path getPath() {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public String getPathAsString() {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public Map<String, Object> getProperties(String schemaName) {
        DataModel dm = getDataModel(schemaName);
        return dm == null ? null : dm.getMap();
    }

    public Object getProperty(String schemaName, String name) {
        DataModel dm = getDataModel(schemaName);
        return dm == null ? null : dm.getData(name);
    }

    public String getTitle() {
        return (String) getProperty("dublincore", "title");
    }

    public DocumentRef getRef() {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public String getSessionId() {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean hasFacet(String facet) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Checks if the given schema has been register before. i.e. a property with
     * the same schema is set onto this object
     */
    public boolean hasSchema(String schema) {
        return dataModels.get(schema) != null;
    }

    public boolean isDownloadable() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isFolder() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isLocked() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isVersionable() {
        // TODO Auto-generated method stub
        return false;
    }

    public void setACP(ACP acp, boolean overwrite) {
        // TODO Auto-generated method stub
    }

    public void setLock(String key) throws ClientException {
        // TODO Auto-generated method stub
    }

    public void setPathInfo(String parentPath, String name) {
        // TODO Auto-generated method stub
    }

    public void setProperties(String schemaName, Map<String, Object> data) {
        getDataModel(schemaName, true).setMap(data);
    }

    public void setProperty(String schemaName, String name, Object value) {
        getDataModel(schemaName, true).setData(name, value);
    }

    public void unlock() throws ClientException {
        // TODO Auto-generated method stub
    }

    public boolean isVersion() {
        // TODO Auto-generated method stub
        return false;
    }

    public ScopedMap getContextData() {
        // TODO Auto-generated method stub
        return null;
    }

    public Serializable getContextData(ScopeType scope, String key) {
        // TODO Auto-generated method stub
        return null;
    }

    public void putContextData(ScopeType scope, String key, Serializable value) {
        // TODO Auto-generated method stub
    }

    public Serializable getContextData(String key) {
        // TODO Auto-generated method stub
        return null;
    }

    public void putContextData(String key, Serializable value) {
        // TODO Auto-generated method stub
    }

    public void copyContextData(DocumentModel otherDocument) {
        // TODO Auto-generated method stub
    }

    public void copyContent(DocumentModel sourceDoc) {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public String getCacheKey() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getRepositoryName() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getSourceId() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getVersionLabel() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see DocumentModel#isProxy()
     */
    public boolean isProxy() {
        return false;
    }

    public DocumentType getDocumentType() {
        return null;
    }

    /**
     * If schema is already registered, leave it so.
     *
     * @param schemas
     */
    public void registerSchemas(String[] schemas) {
        for (String schema : schemas) {
            DataModel dataModel = dataModels.get(schema);
            if (dataModel == null) {
                // log.debug("registering schema: " + schema);
                dataModel = new FictiveDataModel();
                dataModels.put(schema, dataModel);
            }
        }
    }

    public <T> T getAdapter(Class<T> itf, boolean refreshCache) {
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public Map<String, Serializable> getPrefetch() {
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public void prefetchProperty(String id, Object value) {
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public <T extends Serializable> T getSystemProp(String systemProperty,
            Class<T> type) throws ClientException, DocumentException {
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public void prefetchCurrentLifecycleState(String lifecycle) {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public void prefetchLifeCyclePolicy(String lifeCyclePolicy) {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public boolean isLifeCycleLoaded() {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public DocumentPart getPart(String schema) {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public DocumentPart[] getParts() {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public Property getProperty(String xpath) throws PropertyException {

        // tmp hack : use xpath as schemaName:property
        String schemaName = xpath.split(":")[0];
        DocumentPart dp = DefaultPropertyFactory.newDocumentPart(schemaName);
        String prefix = dp.getSchema().getNamespace().prefix;

        Map<String,Serializable> map = new HashMap<String, Serializable>();

        if (!dataModels.containsKey(schemaName))
        {
            String[] schemas = new String[1];
            schemas[0]=schemaName;
            registerSchemas(schemas);
        }

        Map<String,Object> dmMap = dataModels.get(schemaName).getMap();
        for (String k : dmMap.keySet())
        {
            map.put(k, (Serializable)dmMap.get(k));
        }
        dp.init((Serializable) map);
        try
        {
            return dp.resolvePath(xpath);
        }
        catch (PropertyException e) {
            return dp.resolvePath(xpath.replace(schemaName+":", prefix + ":"));
        }


    }

    public Serializable getPropertyValue(String xpath) throws PropertyException {
        return getProperty(xpath).getValue();
    }

    public void setPropertyValue(String xpath, Serializable value)
            throws PropertyException {
        getProperty(xpath).setValue(value);
    }

    public long getFlags() {
        // TODO Auto-generated method stub
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    @Override
    public DocumentModel clone() throws CloneNotSupportedException {
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public void reset() {
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

    public void refresh() throws ClientException {
        throw new java.lang.UnsupportedOperationException("not implemented");

    }

    public void refresh(int refreshFlags, String[] schemas)
            throws ClientException {
        throw new java.lang.UnsupportedOperationException("not implemented");
    }

}
