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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.nuxeo.apidoc.adapters.BaseNuxeoArtifactDocAdapter;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.QueryHelper;
import org.nuxeo.apidoc.api.SeamComponentInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.documentation.JavaDocHelper;
import org.nuxeo.apidoc.introspection.ServerInfo;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.query.sql.NXQL;

public class RepositoryDistributionSnapshot extends BaseNuxeoArtifactDocAdapter implements DistributionSnapshot {

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
            doc.setPropertyValue("dc:title", distrib.getKey());
            doc.setPropertyValue(PROP_KEY, distrib.getKey());
            doc.setPropertyValue(PROP_NAME, distrib.getName());
        } else {
            doc.setPropertyValue("dc:title", label);
            doc.setPropertyValue(PROP_KEY, label + "-" + distrib.getVersion());
            doc.setPropertyValue(PROP_NAME, label);
        }
        doc.setPropertyValue(PROP_LATEST_FT, distrib.isLatestFT());
        doc.setPropertyValue(PROP_LATEST_LTS, distrib.isLatestLTS());
        doc.setPropertyValue(PROP_VERSION, distrib.getVersion());

        DocumentModel ret;
        if (exist) {
            ret = session.saveDocument(doc);
        } else {
            ret = session.createDocument(doc);
        }
        return new RepositoryDistributionSnapshot(ret);
    }

    public static List<DistributionSnapshot> readPersistentSnapshots(CoreSession session) {
        List<DistributionSnapshot> result = new ArrayList<>();
        String query = "SELECT * FROM " + TYPE_NAME + " where ecm:currentLifeCycleState != 'deleted' AND ecm:isVersion = 0";
        DocumentModelList docs = session.query(query);
        for (DocumentModel child : docs) {
            DistributionSnapshot ob = child.getAdapter(DistributionSnapshot.class);
            if (ob != null) {
                result.add(ob);
            }
        }
        return result;
    }

    public RepositoryDistributionSnapshot(DocumentModel doc) {
        super(doc);
    }

    protected <T> List<T> getChildren(Class<T> adapter, String docType) {
        List<T> result = new ArrayList<>();
        String query = QueryHelper.select(docType, doc);
        DocumentModelList docs = getCoreSession().query(query);
        for (DocumentModel child : docs) {
            T ob = child.getAdapter(adapter);
            if (ob != null) {
                result.add(ob);
            }
        }
        return result;
    }

    protected <T> T getChild(Class<T> adapter, String docType, String idField, String id) {
        String query = QueryHelper.select(docType, doc) + " AND " + idField + " = " + NXQL.escapeString(id);
        DocumentModelList docs = getCoreSession().query(query);
        if (docs.isEmpty()) {
            log.error("Unable to find " + docType + " for id " + id);
        } else if (docs.size() == 1) {
            return docs.get(0).getAdapter(adapter);
        } else {
            log.error("multiple match for " + docType + " for id " + id);
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
        return getChild(BundleGroup.class, BundleGroup.TYPE_NAME, BundleGroup.PROP_KEY, groupId);
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
        List<BundleGroup> grps = new ArrayList<>();
        String query = QueryHelper.select(BundleGroup.TYPE_NAME, doc, NXQL.ECM_PARENTID, getBundleContainer().getId());
        DocumentModelList docs = getCoreSession().query(query);
        for (DocumentModel child : docs) {
            BundleGroup bg = child.getAdapter(BundleGroup.class);
            if (bg != null) {
                grps.add(bg);
            }
        }
        return grps;
    }

    @Override
    public List<String> getBundleIds() {
        return getChildren(BundleInfo.class, BundleInfo.TYPE_NAME).stream().map(NuxeoArtifact::getId).collect(
                Collectors.toList());
    }

    @Override
    public ComponentInfo getComponent(String id) {
        return getChild(ComponentInfo.class, ComponentInfo.TYPE_NAME, ComponentInfo.PROP_COMPONENT_ID, id);
    }

    @Override
    public List<String> getComponentIds() {
        return getChildren(ComponentInfo.class, ComponentInfo.TYPE_NAME).stream().map(NuxeoArtifact::getId).collect(
                Collectors.toList());
    }

    @Override
    public ExtensionInfo getContribution(String id) {
        return getChild(ExtensionInfo.class, ExtensionInfo.TYPE_NAME, ExtensionInfo.PROP_CONTRIB_ID, id);
    }

    @Override
    public List<String> getContributionIds() {
        return getChildren(ExtensionInfo.class, ExtensionInfo.TYPE_NAME).stream().map(NuxeoArtifact::getId).collect(
                Collectors.toList());
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
                                                                                  .collect(Collectors.toList());
    }

    @Override
    public List<String> getBundleGroupChildren(String groupId) {
        BundleGroup bg = getChild(BundleGroup.class, BundleGroup.TYPE_NAME, BundleGroup.PROP_KEY, groupId);
        return bg.getBundleIds();
    }

    public List<String> getBundleGroupIds() {
        return getChildren(BundleGroup.class, BundleGroup.TYPE_NAME).stream().map(NuxeoArtifact::getId).collect(
                Collectors.toList());
    }

    @Override
    public List<String> getServiceIds() {
        Set<String> ids = new HashSet<>();
        String query = QueryHelper.select(ComponentInfo.TYPE_NAME, doc);
        DocumentModelList components = getCoreSession().query(query);
        for (DocumentModel componentDoc : components) {
            ComponentInfo ci = componentDoc.getAdapter(ComponentInfo.class);
            if (ci != null) {
                ids.addAll(ci.getServiceNames());
            }
        }
        return new ArrayList<>(ids);
    }

    @Override
    public String getName() {
        try {
            return (String) doc.getPropertyValue(PROP_NAME);
        } catch (PropertyException e) {
            log.error("Error while reading nxdistribution:name", e);
            return "!unknown!";
        }
    }

    @Override
    public String getVersion() {
        try {
            return (String) doc.getPropertyValue(PROP_VERSION);
        } catch (PropertyException e) {
            log.error("Error while reading nxdistribution:version", e);
            return "!unknown!";
        }
    }

    @Override
    public String getKey() {
        try {
            return (String) doc.getPropertyValue(PROP_KEY);
        } catch (PropertyException e) {
            log.error("Error while reading nxdistribution:key", e);
            return "!unknown!";
        }
    }

    @Override
    public List<Class<?>> getSpi() {
        return null;
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
        // Select only not overriden ticket and old imported NXService without overriden value
        String query = QueryHelper.select(ServiceInfo.TYPE_NAME, getDoc()) + " AND " + ServiceInfo.PROP_CLASS_NAME
                + " = " + NXQL.escapeString(id) + " AND (" + ServiceInfo.PROP_OVERRIDEN + " = 0 OR "
                + ServiceInfo.PROP_OVERRIDEN + " is NULL)";
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
                                                                        .collect(Collectors.toList());
    }

    @Override
    public List<String> getXmlComponentIds() {
        return getChildren(ComponentInfo.class, ComponentInfo.TYPE_NAME).stream()
                                                                        .filter(ComponentInfo::isXmlPureComponent)
                                                                        .map(NuxeoArtifact::getId)
                                                                        .collect(Collectors.toList());
    }

    @Override
    public Date getCreationDate() {
        try {
            Calendar cal = (Calendar) getDoc().getPropertyValue("dc:created");
            return cal == null ? null : cal.getTime();
        } catch (PropertyException e) {
            return null;
        }
    }

    @Override
    public Date getReleaseDate() {
        try {
            Calendar cal = (Calendar) getDoc().getPropertyValue("nxdistribution:released");
            return cal == null ? getCreationDate() : cal.getTime();
        } catch (PropertyException e) {
            return null;
        }
    }

    @Override
    public boolean isLive() {
        return false;
    }

    @Override
    public SeamComponentInfo getSeamComponent(String id) {
        String name = id.replace("seam:", "");
        String query = QueryHelper.select(SeamComponentInfo.TYPE_NAME, getDoc()) + " AND "
                + SeamComponentInfo.PROP_COMPONENT_NAME + " = " + NXQL.escapeString(name);
        DocumentModelList docs = getCoreSession().query(query);
        return docs.isEmpty() ? null : docs.get(0).getAdapter(SeamComponentInfo.class);
    }

    @Override
    public List<String> getSeamComponentIds() {
        List<String> result = new ArrayList<>();
        String query = QueryHelper.select(SeamComponentInfo.TYPE_NAME, getDoc());
        DocumentModelList docs = getCoreSession().query(query);
        for (DocumentModel doc : docs) {
            result.add(doc.getAdapter(SeamComponentInfo.class).getId());
        }
        return result;
    }

    @Override
    public List<SeamComponentInfo> getSeamComponents() {
        List<SeamComponentInfo> result = new ArrayList<>();
        String query = QueryHelper.select(SeamComponentInfo.TYPE_NAME, getDoc());
        DocumentModelList docs = getCoreSession().query(query);
        for (DocumentModel doc : docs) {
            result.add(doc.getAdapter(SeamComponentInfo.class));
        }
        return result;
    }

    @Override
    public boolean containsSeamComponents() {
        return getSeamComponentIds().size() > 0;
    }

    @Override
    public OperationInfo getOperation(String id) {
        if (id.startsWith(OperationInfo.ARTIFACT_PREFIX)) {
            id = id.substring(OperationInfo.ARTIFACT_PREFIX.length());
        }
        String query = QueryHelper.select(OperationInfo.TYPE_NAME, getDoc()) + " AND " + OperationInfo.PROP_NAME + " = "
                + NXQL.escapeString(id) + " OR " + OperationInfo.PROP_ALIASES + " = " + NXQL.escapeString(id);
        DocumentModelList docs = getCoreSession().query(query);
        return docs.isEmpty() ? null : docs.get(0).getAdapter(OperationInfo.class);
    }

    @Override
    public List<OperationInfo> getOperations() {
        List<OperationInfo> result = new ArrayList<>();
        String query = QueryHelper.select(OperationInfo.TYPE_NAME, getDoc());
        DocumentModelList docs = getCoreSession().query(query);
        for (DocumentModel doc : docs) {
            result.add(doc.getAdapter(OperationInfo.class));
        }
        // TODO sort
        return result;
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
        List<DocumentRef> refs = new ArrayList<>();
        DocumentModelList docs = getCoreSession().query(query);
        for (DocumentModel doc : docs) {
            refs.add(doc.getRef());
        }
        getCoreSession().removeDocuments(refs.toArray(new DocumentRef[refs.size()]));
    }

    @Override
    public boolean isLatestFT() {
        try {
            return (Boolean) doc.getPropertyValue(PROP_LATEST_FT);
        } catch (PropertyException e) {
            log.error("Error while reading nxdistribution:latestFT", e);
            return false;
        }
    }

    @Override
    public boolean isLatestLTS() {
        try {
            return (Boolean) doc.getPropertyValue(PROP_LATEST_LTS);
        } catch (PropertyException e) {
            log.error("Error while reading nxdistribution:latestLTS", e);
            return false;
        }
    }

    @Override
    public List<String> getAliases() {
        @SuppressWarnings("unchecked")
        List<String> aliases = (List<String>) doc.getPropertyValue(PROP_ALIASES);
        if (isLatestLTS()) {
            aliases.add("latestLTS");
        } else if (isLatestFT()) {
            aliases.add("latestFT");
        }
        return aliases;
    }

    @Override
    public boolean isHidden() {
        return Boolean.TRUE.equals(doc.getPropertyValue(PROP_HIDE));
    }

    @Override
    public ServerInfo getServerInfo() {
        throw new UnsupportedOperationException();
    }
}
