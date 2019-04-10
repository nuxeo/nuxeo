/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.nuxeo.apidoc.adapters.BaseNuxeoArtifactDocAdapter;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.QueryHelper;
import org.nuxeo.apidoc.api.SeamComponentInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.documentation.JavaDocHelper;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.query.sql.NXQL;

import com.google.common.collect.Lists;

public class RepositoryDistributionSnapshot extends BaseNuxeoArtifactDocAdapter
        implements DistributionSnapshot {

    protected JavaDocHelper jdocHelper = null;

    public static RepositoryDistributionSnapshot create(
            DistributionSnapshot distrib, CoreSession session,
            String containerPath, String label) throws ClientException {
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

        doc.setPathInfo(containerPath, name);
        if (label == null) {
            doc.setPropertyValue("dc:title", distrib.getKey());
            doc.setPropertyValue(PROP_KEY, distrib.getKey());
            doc.setPropertyValue(PROP_NAME, distrib.getName());
            doc.setPropertyValue(PROP_VERSION, distrib.getVersion());
        } else {
            doc.setPropertyValue("dc:title", label);
            doc.setPropertyValue(PROP_KEY, label + "-" + distrib.getVersion());
            doc.setPropertyValue(PROP_NAME, label);
            doc.setPropertyValue(PROP_VERSION, distrib.getVersion());
        }

        if (exist) {
            doc = session.saveDocument(doc);
        } else {
            doc = session.createDocument(doc);
        }
        return new RepositoryDistributionSnapshot(doc);
    }

    public static List<DistributionSnapshot> readPersistentSnapshots(
            CoreSession session) {
        List<DistributionSnapshot> result = new ArrayList<DistributionSnapshot>();
        String query = "SELECT * FROM " + TYPE_NAME
                + " where ecm:currentLifeCycleState != 'deleted'";
        try {
            DocumentModelList docs = session.query(query);
            for (DocumentModel child : docs) {
                DistributionSnapshot ob = child.getAdapter(DistributionSnapshot.class);
                if (ob != null) {
                    result.add(ob);
                }
            }
        } catch (Exception e) {
            log.error("Error while executing query " + query, e);
        }
        return result;
    }

    public RepositoryDistributionSnapshot(DocumentModel doc) {
        super(doc);
    }

    protected <T> List<T> getChildren(Class<T> adapter, String docType) {
        List<T> result = new ArrayList<T>();
        String query = QueryHelper.select(docType, doc);
        try {
            DocumentModelList docs = getCoreSession().query(query);
            for (DocumentModel child : docs) {
                T ob = child.getAdapter(adapter);
                if (ob != null) {
                    result.add(ob);
                }
            }
        } catch (Exception e) {
            log.error("Error while executing query " + query, e);
        }
        return result;
    }

    protected <T> T getChild(Class<T> adapter, String docType, String idField,
            String id) {
        String query = QueryHelper.select(docType, doc) + " AND " + idField
                + " = " + NXQL.escapeString(id);
        try {
            DocumentModelList docs = getCoreSession().query(query);
            if (docs.isEmpty()) {
                log.error("Unable to find " + docType + " for id " + id);
            } else if (docs.size() == 1) {
                return docs.get(0).getAdapter(adapter);
            } else {
                log.error("multiple match for " + docType + " for id " + id);
                return docs.get(0).getAdapter(adapter);
            }
        } catch (Exception e) {
            log.error("Error while executing query " + query, e);
        }
        return null;
    }

    @Override
    public BundleInfo getBundle(String id) {
        return getChild(BundleInfo.class, BundleInfo.TYPE_NAME,
                BundleInfo.PROP_BUNDLE_ID, id);
    }

    @Override
    public BundleGroup getBundleGroup(String groupId) {
        return getChild(BundleGroup.class, BundleGroup.TYPE_NAME,
                BundleGroup.PROP_KEY, groupId);
    }

    protected DocumentModel getBundleContainer() {
        try {
            return getCoreSession().getChild(doc.getRef(),
                    SnapshotPersister.Bundle_Root_NAME);
        } catch (ClientException e) {
            // for compatibility with the previous persistence model
            return doc;
        }
    }

    @Override
    public List<BundleGroup> getBundleGroups() {
        List<BundleGroup> grps = new ArrayList<BundleGroup>();
        try {
            String query = QueryHelper.select(BundleGroup.TYPE_NAME, doc,
                    NXQL.ECM_PARENTID, getBundleContainer().getId());
            DocumentModelList docs = getCoreSession().query(query);
            for (DocumentModel child : docs) {
                BundleGroup bg = child.getAdapter(BundleGroup.class);
                if (bg != null) {
                    grps.add(bg);
                }
            }
        } catch (Exception e) {
            log.error("Error while getting bundle groups", e);
        }
        return grps;
    }

    @Override
    public List<String> getBundleIds() {
        List<String> ids = new ArrayList<String>();
        for (BundleInfo bi : getChildren(BundleInfo.class, BundleInfo.TYPE_NAME)) {
            ids.add(bi.getId());
        }
        return ids;
    }

    @Override
    public ComponentInfo getComponent(String id) {
        return getChild(ComponentInfo.class, ComponentInfo.TYPE_NAME,
                ComponentInfo.PROP_COMPONENT_ID, id);
    }

    @Override
    public List<String> getComponentIds() {
        List<String> ids = new ArrayList<String>();
        for (ComponentInfo ci : getChildren(ComponentInfo.class,
                ComponentInfo.TYPE_NAME)) {
            ids.add(ci.getId());
        }
        return ids;
    }

    @Override
    public ExtensionInfo getContribution(String id) {
        return getChild(ExtensionInfo.class, ExtensionInfo.TYPE_NAME,
                ExtensionInfo.PROP_CONTRIB_ID, id);
    }

    @Override
    public List<String> getContributionIds() {
        List<String> ids = new ArrayList<String>();
        for (ExtensionInfo xi : getChildren(ExtensionInfo.class,
                ExtensionInfo.TYPE_NAME)) {
            ids.add(xi.getId());
        }
        return ids;
    }

    @Override
    public List<ExtensionInfo> getContributions() {
        return getChildren(ExtensionInfo.class, ExtensionInfo.TYPE_NAME);
    }

    @Override
    public ExtensionPointInfo getExtensionPoint(String id) {
        return getChild(ExtensionPointInfo.class, ExtensionPointInfo.TYPE_NAME,
                ExtensionPointInfo.PROP_EP_ID, id);
    }

    @Override
    public List<String> getExtensionPointIds() {
        List<String> ids = new ArrayList<String>();
        for (ExtensionPointInfo xpi : getChildren(ExtensionPointInfo.class,
                ExtensionPointInfo.TYPE_NAME)) {
            ids.add(xpi.getId());
        }
        return ids;
    }

    @Override
    public List<String> getBundleGroupChildren(String groupId) {
        BundleGroup bg = getChild(BundleGroup.class, BundleGroup.TYPE_NAME,
                BundleGroup.PROP_KEY, groupId);
        return bg.getBundleIds();
    }

    public List<String> getBundleGroupIds() {
        List<String> ids = new ArrayList<String>();
        for (BundleGroup bg : getChildren(BundleGroup.class,
                BundleGroup.TYPE_NAME)) {
            ids.add(bg.getId());
        }
        return ids;
    }

    @Override
    public List<String> getServiceIds() {
        List<String> ids = new ArrayList<String>();
        try {
            String query = QueryHelper.select(ComponentInfo.TYPE_NAME, doc);
            DocumentModelList components = getCoreSession().query(query);
            for (DocumentModel componentDoc : components) {
                ComponentInfo ci = componentDoc.getAdapter(ComponentInfo.class);
                if (ci != null) {
                    ids.addAll(ci.getServiceNames());
                }
            }
        } catch (Exception e) {
            log.error("Error while getting service ids", e);
        }
        return ids;
    }

    @Override
    public String getName() {
        try {
            return (String) doc.getPropertyValue(PROP_NAME);
        } catch (Exception e) {
            log.error("Error while reading nxdistribution:name", e);
            return "!unknown!";
        }
    }

    @Override
    public String getVersion() {
        try {
            return (String) doc.getPropertyValue(PROP_VERSION);
        } catch (Exception e) {
            log.error("Error while reading nxdistribution:version", e);
            return "!unknown!";
        }
    }

    @Override
    public String getKey() {
        try {
            return (String) doc.getPropertyValue(PROP_KEY);
        } catch (Exception e) {
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
        String query = QueryHelper.select(ServiceInfo.TYPE_NAME, getDoc())
                + " AND " + ServiceInfo.PROP_CLASS_NAME + " = "
                + NXQL.escapeString(id);
        try {
            DocumentModelList docs = getCoreSession().query(query);
            if (docs.size() == 1) {
                return docs.get(0).getAdapter(ServiceInfo.class);
            } else {
                log.error("Multiple services found");
                return null;
            }
        } catch (Exception e) {
            log.error("Unable to fetch NXService", e);
        }
        return null;
    }

    @Override
    public List<String> getJavaComponentIds() {
        List<String> ids = new ArrayList<String>();
        for (ComponentInfo ci : getChildren(ComponentInfo.class,
                ComponentInfo.TYPE_NAME)) {
            if (!ci.isXmlPureComponent()) {
                ids.add(ci.getId());
            }
        }
        return ids;
    }

    @Override
    public List<String> getXmlComponentIds() {
        List<String> ids = new ArrayList<String>();
        for (ComponentInfo ci : getChildren(ComponentInfo.class,
                ComponentInfo.TYPE_NAME)) {
            if (ci.isXmlPureComponent()) {
                ids.add(ci.getId());
            }
        }
        return ids;
    }

    @Override
    public Date getCreationDate() {
        try {
            Calendar cal = (Calendar) getDoc().getPropertyValue("dc:created");
            return cal.getTime();
        } catch (Exception e) {
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
        String query = QueryHelper.select(SeamComponentInfo.TYPE_NAME, getDoc())
                + " AND "
                + SeamComponentInfo.PROP_COMPONENT_NAME
                + " = "
                + NXQL.escapeString(name);
        try {
            DocumentModelList docs = getCoreSession().query(query);
            return docs.get(0).getAdapter(SeamComponentInfo.class);
        } catch (Exception e) {
            log.error("Unable to fetch Seam Component", e);
            return null;
        }
    }

    @Override
    public List<String> getSeamComponentIds() {
        List<String> result = new ArrayList<String>();
        String query = QueryHelper.select(SeamComponentInfo.TYPE_NAME, getDoc());
        try {
            DocumentModelList docs = getCoreSession().query(query);
            for (DocumentModel doc : docs) {
                result.add(doc.getAdapter(SeamComponentInfo.class).getId());
            }
        } catch (Exception e) {
            log.error("Unable to fetch NXService", e);
        }
        return result;
    }

    @Override
    public List<SeamComponentInfo> getSeamComponents() {
        List<SeamComponentInfo> result = new ArrayList<SeamComponentInfo>();
        String query = QueryHelper.select(SeamComponentInfo.TYPE_NAME, getDoc());
        try {
            DocumentModelList docs = getCoreSession().query(query);
            for (DocumentModel doc : docs) {
                result.add(doc.getAdapter(SeamComponentInfo.class));
            }
        } catch (Exception e) {
            log.error("Unable to fetch NXService", e);
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
        String query = QueryHelper.select(OperationInfo.TYPE_NAME, getDoc())
                + " AND " + OperationInfo.PROP_NAME + " = "
                + NXQL.escapeString(id);
        try {
            DocumentModelList docs = getCoreSession().query(query);
            return docs.size() == 0 ? null : docs.get(0).getAdapter(
                    OperationInfo.class);
        } catch (Exception e) {
            log.error("Unable to fetch Seam Component", e);
            return null;
        }
    }

    @Override
    public List<OperationInfo> getOperations() {
        List<OperationInfo> result = new ArrayList<OperationInfo>();
        String query = QueryHelper.select(OperationInfo.TYPE_NAME, getDoc());
        try {
            DocumentModelList docs = getCoreSession().query(query);
            for (DocumentModel doc : docs) {
                result.add(doc.getAdapter(OperationInfo.class));
            }
        } catch (Exception e) {
            log.error("Unable to query", e);
        }
        // TODO sort
        return result;
    }

    @Override
    public JavaDocHelper getJavaDocHelper() {
        if (jdocHelper == null) {
            jdocHelper = JavaDocHelper.getHelper(getName(), getVersion());
        }
        return jdocHelper;
    }

    @Override
    public void cleanPreviousArtifacts() {
        String query = QueryHelper.select("Document", getDoc());
        try {
            List<DocumentRef> refs = new ArrayList<>();
            DocumentModelList docs = getCoreSession().query(query);
            for(DocumentModel doc : docs) {
                refs.add(doc.getRef());
            }
            getCoreSession().removeDocuments(refs.toArray(new DocumentRef[1]));
        } catch (ClientException e) {
            log.warn("Unable to query", e);
        }
    }
}
