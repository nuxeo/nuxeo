/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.repository;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.apidoc.adapters.BaseNuxeoArtifactDocAdapter;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.QueryHelper;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.documentation.JavaDocHelper;
import org.nuxeo.apidoc.plugin.Plugin;
import org.nuxeo.apidoc.plugin.PluginSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RepositoryDistributionSnapshot extends BaseNuxeoArtifactDocAdapter implements DistributionSnapshot {

    private static final Logger log = LogManager.getLogger(RepositoryDistributionSnapshot.class);

    protected JavaDocHelper jdocHelper = null;

    public static RepositoryDistributionSnapshot create(DistributionSnapshot distrib, CoreSession session,
            String containerPath, String label, Map<String, Serializable> properties) {
        DocumentModel doc = session.createDocumentModel(TYPE_NAME);
        String name = computeDocumentName(distrib.getKey());
        if (label != null) {
            name = computeDocumentName(label);
        }
        String targetPath = new Path(containerPath).append(name).toString();

        boolean exist = false;
        if (session.exists(new PathRef(targetPath))) {
            exist = true;
            doc = session.getDocument(new PathRef(targetPath));
        }

        // Set first properties passed by parameter to not override default
        // behavior
        if (properties != null) {
            properties.forEach(doc::setPropertyValue);
        }

        doc.setPathInfo(containerPath, name);
        if (label == null) {
            doc.setPropertyValue(NuxeoArtifact.TITLE_PROPERTY_PATH, distrib.getKey());
            doc.setPropertyValue(PROP_KEY, distrib.getKey());
            doc.setPropertyValue(PROP_NAME, distrib.getName());
        } else {
            doc.setPropertyValue(NuxeoArtifact.TITLE_PROPERTY_PATH, label);
            doc.setPropertyValue(PROP_KEY, label + "-" + distrib.getVersion());
            doc.setPropertyValue(PROP_NAME, label);
        }
        doc.setPropertyValue(PROP_LATEST_FT, distrib.isLatestFT());
        doc.setPropertyValue(PROP_LATEST_LTS, distrib.isLatestLTS());
        doc.setPropertyValue(PROP_VERSION, distrib.getVersion());

        DocumentModel ret;
        doc.putContextData(ThumbnailConstants.DISABLE_THUMBNAIL_COMPUTATION, true);
        if (exist) {
            ret = session.saveDocument(doc);
        } else {
            ret = session.createDocument(doc);
        }
        return new RepositoryDistributionSnapshot(ret);
    }

    public static List<DistributionSnapshot> readPersistentSnapshots(CoreSession session) {
        String query = String.format("SELECT * FROM %s where %s AND %s", TYPE_NAME, QueryHelper.NOT_DELETED,
                QueryHelper.NOT_VERSION);
        DocumentModelList docs = session.query(query);
        return docs.stream()
                   .map(doc -> doc.getAdapter(DistributionSnapshot.class))
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
    }

    public RepositoryDistributionSnapshot(DocumentModel doc) {
        super(doc);
    }

    protected <T> List<T> getChildren(Class<T> adapter, String docType) {
        String query = QueryHelper.select(docType, doc, NXQL.ECM_POS);
        DocumentModelList docs = getCoreSession().query(query);
        return docs.stream().map(doc -> doc.getAdapter(adapter)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    protected <T> T getChild(Class<T> adapter, String docType, String idField, String id) {
        String query = QueryHelper.select(docType, doc, idField, id);
        DocumentModelList docs = getCoreSession().query(query);
        if (docs.isEmpty()) {
            log.error(String.format("Unable to find %s with id '%s'", docType, id));
        } else if (docs.size() == 1) {
            return docs.get(0).getAdapter(adapter);
        } else {
            log.error(String.format("Multiple match for %s with id '%s'", docType, id));
            return docs.get(0).getAdapter(adapter);
        }
        return null;
    }

    @Override
    public BundleInfo getBundle(String id) {
        return getChild(BundleInfo.class, BundleInfo.TYPE_NAME, BundleInfo.PROP_BUNDLE_ID, id);
    }

    @Override
    public BundleGroup getBundleGroup(String groupId) {
        if (groupId == null) {
            return null;
        }
        if (groupId.startsWith(BundleGroup.PREFIX)) {
            return getChild(BundleGroup.class, BundleGroup.TYPE_NAME, BundleGroup.PROP_KEY, groupId);
        } else {
            return getChild(BundleGroup.class, BundleGroup.TYPE_NAME, BundleGroup.PROP_GROUP_NAME, groupId);
        }
    }

    protected DocumentModel getBundleContainer() {
        DocumentRef ref = new PathRef(doc.getPathAsString(), SnapshotPersister.Bundle_Root_NAME);
        if (getCoreSession().exists(ref)) {
            return getCoreSession().getDocument(ref);
        } else {
            // for compatibility with the previous persistence model
            return doc;
        }
    }

    @Override
    public List<BundleGroup> getBundleGroups() {
        String query = QueryHelper.select(BundleGroup.TYPE_NAME, doc, NXQL.ECM_PARENTID, getBundleContainer().getId());
        DocumentModelList docs = getCoreSession().query(query);
        return docs.stream()
                   .map(doc -> doc.getAdapter(BundleGroup.class))
                   .filter(Objects::nonNull)
                   .sorted(Comparator.comparing(BundleGroup::getId))
                   .collect(Collectors.toList());
    }

    @Override
    public List<BundleInfo> getBundles() {
        return getChildren(BundleInfo.class, BundleInfo.TYPE_NAME);
    }

    @Override
    public List<String> getBundleIds() {
        return getChildren(BundleInfo.class, BundleInfo.TYPE_NAME).stream()
                                                                  .map(NuxeoArtifact::getId)
                                                                  .sorted()
                                                                  .collect(Collectors.toList());
    }

    @Override
    public ComponentInfo getComponent(String id) {
        return getChild(ComponentInfo.class, ComponentInfo.TYPE_NAME, ComponentInfo.PROP_COMPONENT_ID, id);
    }

    @Override
    public List<String> getComponentIds() {
        return getChildren(ComponentInfo.class, ComponentInfo.TYPE_NAME).stream()
                                                                        .map(NuxeoArtifact::getId)
                                                                        .sorted()
                                                                        .collect(Collectors.toList());
    }

    @Override
    public ExtensionInfo getContribution(String id) {
        return getChild(ExtensionInfo.class, ExtensionInfo.TYPE_NAME, ExtensionInfo.PROP_CONTRIB_ID, id);
    }

    @Override
    public List<String> getContributionIds() {
        return getChildren(ExtensionInfo.class, ExtensionInfo.TYPE_NAME).stream()
                                                                        .map(NuxeoArtifact::getId)
                                                                        .sorted()
                                                                        .collect(Collectors.toList());
    }

    @Override
    public List<ExtensionInfo> getContributions() {
        return getChildren(ExtensionInfo.class, ExtensionInfo.TYPE_NAME);
    }

    @Override
    public ExtensionPointInfo getExtensionPoint(String id) {
        return getChild(ExtensionPointInfo.class, ExtensionPointInfo.TYPE_NAME, ExtensionPointInfo.PROP_EP_ID, id);
    }

    @Override
    public List<String> getExtensionPointIds() {
        return getChildren(ExtensionPointInfo.class, ExtensionPointInfo.TYPE_NAME).stream()
                                                                                  .map(NuxeoArtifact::getId)
                                                                                  .sorted()
                                                                                  .collect(Collectors.toList());
    }

    public List<String> getBundleGroupIds() {
        return getChildren(BundleGroup.class, BundleGroup.TYPE_NAME).stream()
                                                                    .map(NuxeoArtifact::getId)
                                                                    .sorted()
                                                                    .collect(Collectors.toList());
    }

    @Override
    public List<String> getServiceIds() {
        String query = QueryHelper.select(ComponentInfo.TYPE_NAME, doc);
        DocumentModelList components = getCoreSession().query(query);
        return components.stream()
                         .map(doc -> doc.getAdapter(ComponentInfo.class))
                         .filter(Objects::nonNull)
                         .map(ComponentInfo::getServiceNames)
                         .flatMap(Collection::stream)
                         .sorted()
                         .collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return safeGet(PROP_NAME, "!unknown!");
    }

    @Override
    public String getVersion() {
        return safeGet(PROP_VERSION, "!unknown!");
    }

    @Override
    public String getKey() {
        return safeGet(PROP_KEY, "!unknown!");
    }

    @Override
    public String getId() {
        return getKey();
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public ServiceInfo getService(String id) {
        // Select only not overridden ticket and old imported NXService without overridden value
        String query = String.format("%s AND (%s = 0 OR %s is NULL)",
                QueryHelper.select(ServiceInfo.TYPE_NAME, getDoc(), ServiceInfo.PROP_CLASS_NAME, id),
                ServiceInfo.PROP_OVERRIDEN, ServiceInfo.PROP_OVERRIDEN);
        DocumentModelList docs = getCoreSession().query(query);
        if (docs.size() > 1) {
            throw new AssertionError("Multiple services found for " + id);
        }
        return docs.get(0).getAdapter(ServiceInfo.class);
    }

    @Override
    public List<String> getJavaComponentIds() {
        return getChildren(ComponentInfo.class, ComponentInfo.TYPE_NAME).stream()
                                                                        .filter(ci -> !ci.isXmlPureComponent())
                                                                        .map(NuxeoArtifact::getId)
                                                                        .sorted()
                                                                        .collect(Collectors.toList());
    }

    @Override
    public List<String> getXmlComponentIds() {
        return getChildren(ComponentInfo.class, ComponentInfo.TYPE_NAME).stream()
                                                                        .filter(ComponentInfo::isXmlPureComponent)
                                                                        .map(NuxeoArtifact::getId)
                                                                        .sorted()
                                                                        .collect(Collectors.toList());
    }

    @Override
    public Date getCreationDate() {
        Calendar cal = safeGet(PROP_CREATED);
        return cal == null ? null : cal.getTime();
    }

    @Override
    public Date getReleaseDate() {
        Calendar cal = safeGet(PROP_RELEASED);
        return cal == null ? getCreationDate() : cal.getTime();
    }

    @Override
    public boolean isLive() {
        return false;
    }

    @Override
    public OperationInfo getOperation(String id) {
        if (id.startsWith(OperationInfo.ARTIFACT_PREFIX)) {
            id = id.substring(OperationInfo.ARTIFACT_PREFIX.length());
        }
        String query = String.format("%s OR %s = %s",
                QueryHelper.select(OperationInfo.TYPE_NAME, getDoc(), OperationInfo.PROP_NAME, id),
                OperationInfo.PROP_ALIASES, NXQL.escapeString(id));
        DocumentModelList docs = getCoreSession().query(query);
        return docs.isEmpty() ? null : docs.get(0).getAdapter(OperationInfo.class);
    }

    @Override
    public List<OperationInfo> getOperations() {
        String query = QueryHelper.select(OperationInfo.TYPE_NAME, getDoc(), NXQL.ECM_POS);
        DocumentModelList docs = getCoreSession().query(query);
        return docs.stream()
                   .map(doc -> doc.getAdapter(OperationInfo.class))
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
    }

    public JavaDocHelper getJavaDocHelper() {
        if (jdocHelper == null) {
            jdocHelper = JavaDocHelper.getHelper(getName(), getVersion());
        }
        return jdocHelper;
    }

    @Override
    public void cleanPreviousArtifacts() {
        String query = QueryHelper.select("Document", getDoc());
        DocumentModelList docs = getCoreSession().query(query);
        getCoreSession().removeDocuments(docs.stream().map(doc -> doc.getRef()).toArray(size -> new DocumentRef[size]));
    }

    @Override
    public boolean isLatestFT() {
        return safeGet(PROP_LATEST_FT);
    }

    @Override
    public boolean isLatestLTS() {
        return safeGet(PROP_LATEST_LTS);
    }

    @Override
    public List<String> getAliases() {
        List<String> aliases = safeGet(PROP_ALIASES);
        if (isLatestLTS()) {
            aliases.add("latestLTS");
        } else if (isLatestFT()) {
            aliases.add("latestFT");
        }
        return aliases;
    }

    @Override
    public boolean isHidden() {
        return Boolean.TRUE.equals(safeGet(PROP_HIDE));
    }

    @Override
    public ObjectMapper getJsonMapper() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeJson(OutputStream out) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DistributionSnapshot readJson(InputStream in) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, PluginSnapshot<?>> getPluginSnapshots() {
        return Framework.getService(SnapshotManager.class)
                        .getPlugins()
                        .stream()
                        .collect(Collectors.toMap(Plugin::getId, p -> p.getRepositorySnapshot(getDoc())));
    }
}
