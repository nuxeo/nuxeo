/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.webapp.bulkedit;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
import org.nuxeo.ecm.core.api.impl.DataModelMapImpl;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.impl.DefaultPropertyFactory;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.runtime.api.Framework;

/**
 * A DocumentModel that can have any schema and is not made persistent by
 * itself. A mockup to keep arbitrary schema data.
 *
 * @author DM
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class FictiveDocumentModel implements DocumentModel {

    private static final long serialVersionUID = 1L;

    protected final DataModelMap dataModels = new DataModelMapImpl();

    protected final ScopedMap contextData = new ScopedMap();

    public static DocumentModel createFictiveDocumentModelWith(
            List<String> schemas) {
        FictiveDocumentModel doc = new FictiveDocumentModel();
        for (String schema : schemas) {
            DataModel dataModel = doc.dataModels.get(schema);
            if (dataModel == null) {
                dataModel = new FictiveDataModel(schema);
                doc.dataModels.put(schema, dataModel);
            }
        }
        return doc;
    }

    public static DocumentModel createFictiveDocumentModelWith(
            String... schemas) {
        return createFictiveDocumentModelWith(Arrays.asList(schemas));
    }

    protected FictiveDocumentModel() {
    }

    public String[] getDeclaredSchemas() {
        Set<String> keys = dataModels.keySet();
        return keys.toArray(new String[keys.size()]);
    }

    public Object getProperty(String schemaName, String name)
            throws ClientException {
        DataModel dm = dataModels.get(schemaName);
        return dm != null ? dm.getData(name) : null;
    }

    public void setProperty(String schemaName, String name, Object value)
            throws ClientException {
        if (name.contains(":")) {
            name = name.substring(name.indexOf(":"), name.length());
        }
        dataModels.get(schemaName).setData(name, value);
    }

    public Map<String, Object> getProperties(String schemaName)
            throws ClientException {
        return dataModels.get(schemaName).getMap();
    }

    public void setProperties(String schemaName, Map<String, Object> data)
            throws ClientException {
        dataModels.get(schemaName).setMap(data);
    }

    public ScopedMap getContextData() {
        return contextData;
    }

    public Serializable getContextData(ScopeType scope, String key) {
        return contextData.getScopedValue(scope, key);
    }

    public void putContextData(ScopeType scope, String key, Serializable value) {
        contextData.putScopedValue(scope, key, value);
    }

    public Serializable getContextData(String key) {
        return contextData.getScopedValue(key);
    }

    public void putContextData(String key, Serializable value) {
        contextData.putScopedValue(key, value);
    }

    public void copyContextData(DocumentModel otherDocument) {
        ScopedMap otherMap = otherDocument.getContextData();
        if (otherMap != null) {
            contextData.putAll(otherMap);
        }
    }

    public Property getProperty(String xpath) throws PropertyException,
            ClientException {
        Path path = new Path(xpath);
        if (path.segmentCount() == 0) {
            throw new PropertyNotFoundException(xpath, "Schema not specified");
        }
        String segment = path.segment(0);
        int p = segment.indexOf(':');
        if (p == -1) { // support also other schema paths? like schema.property
            // allow also unprefixed schemas -> make a search for the first
            // matching schema having a property with same name as path segment
            // 0
            DocumentPart[] parts = getParts();
            for (DocumentPart part : parts) {
                if (part.getSchema().hasField(segment)) {
                    return part.resolvePath(path.toString());
                }
            }
            // could not find any matching schema
            throw new PropertyNotFoundException(xpath, "Schema not specified");
        }
        String prefix = segment.substring(0, p);
        SchemaManager mgr = Framework.getLocalService(SchemaManager.class);
        Schema schema = mgr.getSchemaFromPrefix(prefix);
        if (schema == null) {
            schema = mgr.getSchema(prefix);
            if (schema == null) {
                throw new PropertyNotFoundException(xpath,
                        "Could not find registered schema with prefix: "
                                + prefix);
            }
        }
        String[] segments = path.segments();
        segments[0] = segments[0].substring(p + 1);
        path = Path.createFromSegments(segments);

        DocumentPart part = DefaultPropertyFactory.newDocumentPart(schema);
        part.init((Serializable) dataModels.get(schema.getName()).getMap());
        return part.resolvePath(path.toString());
    }

    public Serializable getPropertyValue(String xpath)
            throws PropertyException, ClientException {
        return getProperty(xpath).getValue();
    }

    public void setPropertyValue(String xpath, Serializable value)
            throws PropertyException, ClientException {
        getProperty(xpath).setValue(value);
    }

    public DocumentType getDocumentType() {
        throw new UnsupportedOperationException();
    }

    public String getSessionId() {
        throw new UnsupportedOperationException();
    }

    public CoreSession getCoreSession() {
        throw new UnsupportedOperationException();
    }

    public DocumentRef getRef() {
        throw new UnsupportedOperationException();
    }

    public DocumentRef getParentRef() {
        throw new UnsupportedOperationException();
    }

    public String getId() {
        throw new UnsupportedOperationException();
    }

    public String getName() {
        throw new UnsupportedOperationException();
    }

    public String getTitle() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public String getPathAsString() {
        throw new UnsupportedOperationException();
    }

    public Path getPath() {
        throw new UnsupportedOperationException();
    }

    public String getType() {
        throw new UnsupportedOperationException();
    }

    public Set<String> getDeclaredFacets() {
        throw new UnsupportedOperationException();
    }

    public Collection<DataModel> getDataModelsCollection() {
        throw new UnsupportedOperationException();
    }

    public DataModelMap getDataModels() {
        throw new UnsupportedOperationException();
    }

    public DataModel getDataModel(String schema) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void setPathInfo(String parentPath, String name) {
        throw new UnsupportedOperationException();
    }

    public String getLock() {
        throw new UnsupportedOperationException();
    }

    public boolean isLocked() {
        throw new UnsupportedOperationException();
    }

    public void setLock(String key) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void unlock() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public ACP getACP() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void setACP(ACP acp, boolean overwrite) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public boolean hasSchema(String schema) {
        throw new UnsupportedOperationException();
    }

    public boolean hasFacet(String facet) {
        throw new UnsupportedOperationException();
    }

    public boolean isFolder() {
        throw new UnsupportedOperationException();
    }

    public boolean isVersionable() {
        throw new UnsupportedOperationException();
    }

    public boolean isDownloadable() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public boolean isVersion() {
        throw new UnsupportedOperationException();
    }

    public boolean isProxy() {
        throw new UnsupportedOperationException();
    }

    public boolean isImmutable() {
        throw new UnsupportedOperationException();
    }

    public <T> T getAdapter(Class<T> itf) {
        throw new UnsupportedOperationException();
    }

    public <T> T getAdapter(Class<T> itf, boolean refreshCache) {
        throw new UnsupportedOperationException();
    }

    public String getCurrentLifeCycleState() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public String getLifeCyclePolicy() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public boolean followTransition(String transition) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public Collection<String> getAllowedStateTransitions()
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void copyContent(DocumentModel sourceDoc) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public String getRepositoryName() {
        throw new UnsupportedOperationException();
    }

    public String getCacheKey() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public String getSourceId() {
        throw new UnsupportedOperationException();
    }

    public String getVersionLabel() {
        throw new UnsupportedOperationException();
    }

    public Map<String, Serializable> getPrefetch() {
        throw new UnsupportedOperationException();
    }

    public void prefetchProperty(String id, Object value) {
        throw new UnsupportedOperationException();
    }

    public void prefetchCurrentLifecycleState(String lifecycle) {
        throw new UnsupportedOperationException();
    }

    public void prefetchLifeCyclePolicy(String lifeCyclePolicy) {
        throw new UnsupportedOperationException();
    }

    public boolean isLifeCycleLoaded() {
        throw new UnsupportedOperationException();
    }

    public <T extends Serializable> T getSystemProp(String systemProperty,
            Class<T> type) throws ClientException, DocumentException {
        throw new UnsupportedOperationException();
    }

    public DocumentPart getPart(String schema) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public DocumentPart[] getParts() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public long getFlags() {
        throw new UnsupportedOperationException();
    }

    public void reset() {
        throw new UnsupportedOperationException();
    }

    public void refresh(int refreshFlags, String[] schemas)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void refresh() throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel clone() throws CloneNotSupportedException {
        throw new UnsupportedOperationException();
    }

}
