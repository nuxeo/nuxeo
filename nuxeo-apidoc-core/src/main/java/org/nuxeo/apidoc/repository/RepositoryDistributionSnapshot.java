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
package org.nuxeo.apidoc.repository;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.apidoc.adapters.BaseNuxeoArtifactDocAdapter;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.introspection.ServiceInfoImpl;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;

/**
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
public class RepositoryDistributionSnapshot extends BaseNuxeoArtifactDocAdapter implements DistributionSnapshot {


    public static RepositoryDistributionSnapshot create(DistributionSnapshot distrib, CoreSession session, String containerPath) throws ClientException {
        DocumentModel doc = session.createDocumentModel(TYPE_NAME);
        String name = computeDocumentName(distrib.getKey());
        String targetPath = new Path(containerPath).append(name).toString();
        boolean exist = false;
        if (session.exists(new PathRef(targetPath))) {
            exist = true;
            doc = session.getDocument(new PathRef(targetPath));
        }
        doc.setPathInfo(containerPath, name);
        doc.setPropertyValue("dc:title", distrib.getKey());
        doc.setPropertyValue("nxdistribution:name", distrib.getName());
        doc.setPropertyValue("nxdistribution:version", distrib.getVersion());
        doc.setPropertyValue("nxdistribution:key", distrib.getKey());
        if (exist) {
            doc = session.saveDocument(doc);
        } else {
            doc = session.createDocument(doc);
        }
        return new RepositoryDistributionSnapshot(doc);
    }

    public static List<DistributionSnapshot> readPersistentSnapshots(CoreSession session) {
        List<DistributionSnapshot> result = new ArrayList<DistributionSnapshot>();
        String query = "select * from " + TYPE_NAME ;
        try {

            DocumentModelList docs = session.query(query);
            for (DocumentModel child : docs) {
                DistributionSnapshot ob = child.getAdapter(DistributionSnapshot.class);
                if (ob!=null) {
                    result.add(ob);
                }
            }
        }
        catch (Exception e) {
            log.error("Error while executing query " + query ,e);
        }
        return result;
    }

    public RepositoryDistributionSnapshot(DocumentModel doc) {
        super(doc);
    }

    protected <T> List<T> getChildren(Class<T> adapter, String docType) {
        List<T> result = new ArrayList<T>();
        String query = "select * from " + docType + " where ecm:path startswith '" + doc.getPathAsString() + "'";
        try {
            DocumentModelList docs = getCoreSession().query(query);
            for (DocumentModel child : docs) {
                T ob = child.getAdapter(adapter);
                if (ob!=null) {
                    result.add(ob);
                }
            }
        }
        catch (Exception e) {
            log.error("Error while executing query " + query ,e);
        }
        return result;
    }


    protected <T> T getChild(Class<T> adapter, String docType, String idField, String id) {
        T result = null;
        String query = "select * from " + docType + " where ecm:path startswith '" + doc.getPathAsString() + "' AND " + idField +"= '" + id + "'";
        try {
            DocumentModelList docs = getCoreSession().query(query);
            if (docs.size()==0) {
                log.error("Unable to find " + docType + " for id " + id);
            } else if (docs.size()==1) {
                return docs.get(0).getAdapter(adapter);
            } else {
                log.error("multiple match for " + docType + " for id " + id);
                return docs.get(0).getAdapter(adapter);
            }
        }
        catch (Exception e) {
            log.error("Error while executing query " + query ,e);
        }
        return result;
    }


    public BundleInfo getBundle(String id) {
        return getChild(BundleInfo.class, BundleInfo.TYPE_NAME, "nxbundle:bundleId", id);
    }

    public BundleGroup getBundleGroup(String groupId) {
        return getChild(BundleGroup.class, BundleGroup.TYPE_NAME, "nxbundlegroup:key", groupId);
    }

    public List<BundleGroup> getBundleGroups() {
        List<BundleGroup> grps = new ArrayList<BundleGroup>();
        try {
            String query = "select * from NXBundleGroup where ecm:path startswith '" + doc.getPathAsString() + "'";

            DocumentModelList docs = getCoreSession().query(query);
            for (DocumentModel child : docs) {
                BundleGroup bg = child.getAdapter(BundleGroup.class);
                if (bg!=null) {
                    grps.add(bg);
                }
            }
        }
        catch (Exception e) {
            log.error("Error while getting bundle groups",e);
        }
        return grps;
    }

    public List<String> getBundleIds() {
        List<String> ids = new ArrayList<String>();
        for (BundleInfo bi : getChildren(BundleInfo.class, BundleInfo.TYPE_NAME)) {
            ids.add(bi.getId());
        }
        return ids;
    }

    public ComponentInfo getComponent(String id) {
        return getChild(ComponentInfo.class, ComponentInfo.TYPE_NAME, "nxcomponent:componentId", id);
    }

    public List<String> getComponentIds() {
        List<String> ids = new ArrayList<String>();
        for (ComponentInfo ci : getChildren(ComponentInfo.class, ComponentInfo.TYPE_NAME)) {
            ids.add(ci.getId());
        }
        return ids;
    }

    public ExtensionInfo getContribution(String id) {
        return getChild(ExtensionInfo.class, ExtensionInfo.TYPE_NAME, "nxcontribution:contribId", id);
    }

    public List<String> getContributionIds() {
        List<String> ids = new ArrayList<String>();
        for (ExtensionInfo xi : getChildren(ExtensionInfo.class, ExtensionInfo.TYPE_NAME)) {
            ids.add(xi.getId());
        }
        return ids;
    }

    public ExtensionPointInfo getExtensionPoint(String id) {
        return getChild(ExtensionPointInfo.class, ExtensionPointInfo.TYPE_NAME, "nxextensionpoint:epId", id);
    }

    public List<String> getExtensionPointIds() {
        List<String> ids = new ArrayList<String>();
        for (ExtensionPointInfo xpi : getChildren(ExtensionPointInfo.class, ExtensionPointInfo.TYPE_NAME)) {
            ids.add(xpi.getId());
        }
        return ids;
    }

    public List<String> getBundleGroupChildren(String groupId) {
        BundleGroup bg = getChild(BundleGroup.class, BundleGroup.TYPE_NAME, "nxbundlegroup:key", groupId);
        return bg.getBundleIds();
    }

    public List<String> getBundleGroupIds() {
         List<String> ids = new ArrayList<String>();
         for (BundleGroup bg : getChildren(BundleGroup.class, BundleGroup.TYPE_NAME)) {
             ids.add(bg.getId());
         }
         return ids;
    }

    public List<String> getServiceIds() {
        List<String> ids = new ArrayList<String>();
        try {
            String query = "select * from NXComponent where ecm:path startswith '" + doc.getPathAsString() + "'";

            DocumentModelList components = getCoreSession().query(query);
            for (DocumentModel componentDoc : components) {
                ComponentInfo ci = componentDoc.getAdapter(ComponentInfo.class);
                if (ci!=null) {
                    ids.addAll(ci.getServiceNames());
                }
            }
        }
        catch (Exception e) {
            log.error("Error while getting service ids",e);
        }
        return ids;
    }

    public String getName() {
        try {
            return (String) doc.getPropertyValue("nxdistribution:name");
        }
        catch (Exception e) {
            log.error("Error while reading nxdistribution:name",e);
            return "!unknown!";
        }
    }

    public String getVersion() {
         try {
             return (String) doc.getPropertyValue("nxdistribution:version");
         }
         catch (Exception e) {
             log.error("Error while reading nxdistribution:version",e);
             return "!unknown!";
         }
     }

    public String getKey() {
        try {
            return (String) doc.getPropertyValue("nxdistribution:key");
        }
        catch (Exception e) {
            log.error("Error while reading nxdistribution:key",e);
            return "!unknown!";
        }
    }

    public DistributionSnapshot persist(CoreSession session) throws ClientException {
        session.save();
        return null;
    }

    public List<Class> getSpi() {
        return null;
    }

    @Override
    public String getId() {
        return getKey();
    }

    public String getArtifactType() {
        return DistributionSnapshot.TYPE_NAME;
    }

    public ServiceInfo getService(String id) {

        String startPath = getDoc().getPathAsString();

        String query = "select * from NXService where nxservice:className='" + id + "' AND ecm:path STARTSWITH '" + startPath + "/'";

        try {
            DocumentModelList docs = getCoreSession().query(query);
            if (docs.size()==1) {
                return docs.get(0).getAdapter(ServiceInfo.class);
            } else {
                log.error("Multiple services found");
                return null;
            }
        }
        catch (Exception e) {
            log.error("Unable to fetch NXService",e);
        }
        return null;
    }

}
