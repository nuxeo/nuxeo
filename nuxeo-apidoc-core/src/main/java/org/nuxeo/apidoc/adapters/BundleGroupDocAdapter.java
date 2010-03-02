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
package org.nuxeo.apidoc.adapters;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
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
public class BundleGroupDocAdapter extends BaseNuxeoArtifactDocAdapter
        implements BundleGroup {

    public static BundleGroupDocAdapter create(BundleGroup bundleGroup, CoreSession session, String containerPath) throws ClientException {

        DocumentModel doc = session.createDocumentModel(TYPE_NAME);
        String name = computeDocumentName("bg-" + bundleGroup.getId());
        String targetPath = new Path(containerPath).append(name).toString();
        boolean exist = false;
        if (session.exists(new PathRef(targetPath))) {
            exist = true;
            doc = session.getDocument(new PathRef(targetPath));
        }
        doc.setPathInfo(containerPath, name);
        doc.setPropertyValue("dc:title", bundleGroup.getName());
        doc.setPropertyValue("nxbundlegroup:groupName", bundleGroup.getName());
        doc.setPropertyValue("nxbundlegroup:key", bundleGroup.getId());
        if (exist) {
            doc = session.saveDocument(doc);
        } else {
            doc = session.createDocument(doc);
        }
        return new BundleGroupDocAdapter(doc);
    }

    public BundleGroupDocAdapter(DocumentModel doc) {
        super(doc);
    }

    public List<String> getBundleIds() {
        List<String> bundles = new ArrayList<String>();

        String query = "select * from NXBundleGroup where ecm:path STARTSWITH '" + doc.getPathAsString() + "'";
         try {
             DocumentModelList docs = getCoreSession().query(query);
             for(DocumentModel child : docs) {
                 BundleInfo bi = child.getAdapter(BundleInfo.class);
                 if (bi!=null) {
                     bundles.add(bi.getId());
                 }
                 }
         }
         catch (Exception e) {
             log.error("Error while getting subGroups",e);
         }
        return bundles;
    }

    private String getKey() {
        return safeGet("nxbundlegroup:key", "unknown_bundle_group");
    }

    public String getName() {
        return safeGet("nxbundlegroup:groupName", "unknow_bundle_group");
    }

    public List<BundleGroup> getSubGroups() {

        List<BundleGroup> grps = new ArrayList<BundleGroup>();

        String query = "select * from NXBundleGroup where ecm:path STARTSWITH '" + doc.getPathAsString() + "'";
         try {
             DocumentModelList docs = getCoreSession().query(query);
             for(DocumentModel child : docs) {
                 BundleGroup grp = child.getAdapter(BundleGroup.class);
                 if (grp!=null) {
                     grps.add(grp);
                 }
                 }
         }
         catch (Exception e) {
             log.error("Error while getting subGroups",e);
         }
        return grps;
    }

    @Override
    public String getId() {
        return getKey();
    }

    public String getVersion() {

        DistributionSnapshot parentSnapshot = getParentNuxeoArtifact(DistributionSnapshot.class);

        if (parentSnapshot!=null) {
            return parentSnapshot.getVersion();
        }

        log.error("Unable to determine version for bundleGroup " + getId());
        return "?";
    }

    public String getArtifactType() {
        return BundleGroup.TYPE_NAME;
    }

}
