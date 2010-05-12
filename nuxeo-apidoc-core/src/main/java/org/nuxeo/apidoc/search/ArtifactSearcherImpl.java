/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.apidoc.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.adapters.BaseNuxeoArtifactDocAdapter;
import org.nuxeo.apidoc.adapters.BundleGroupDocAdapter;
import org.nuxeo.apidoc.adapters.BundleInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ComponentInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ExtensionInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ExtensionPointInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ServiceInfoDocAdapter;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.repository.RepositoryDistributionSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.runtime.api.Framework;

public class ArtifactSearcherImpl implements ArtifactSearcher {

    protected NuxeoArtifact mapDoc2Artifact(DocumentModel doc) {
        NuxeoArtifact artifact = null;

        if (doc.getType().equals(BundleGroup.TYPE_NAME)) {
            artifact=  new BundleGroupDocAdapter(doc);
        } else if (doc.getType().equals(BundleInfo.TYPE_NAME)) {
            artifact =  new BundleInfoDocAdapter(doc);
        } else if (doc.getType().equals(ComponentInfo.TYPE_NAME)) {
            artifact =  new ComponentInfoDocAdapter(doc);
        } else if (doc.getType().equals(ExtensionPointInfo.TYPE_NAME)) {
            artifact =  new ExtensionPointInfoDocAdapter(doc);
        } else  if (doc.getType().equals(ExtensionInfo.TYPE_NAME)) {
            artifact =  new ExtensionInfoDocAdapter(doc);
        } else  if (doc.getType().equals(DistributionSnapshot.TYPE_NAME)) {
            artifact = new RepositoryDistributionSnapshot(doc);
        } else if (doc.getType().equals(ServiceInfo.TYPE_NAME)) {
            artifact =  new ServiceInfoDocAdapter(doc);
        }

        return artifact;
    }

    public List<NuxeoArtifact> searchArtifact(CoreSession session, String fulltext) throws Exception {

        List<NuxeoArtifact> result = new ArrayList<NuxeoArtifact>();

        String query = "select * from Document where ecm:primaryType IN ('NXDistribution','NXBundleGroup', 'NXBundle', 'NXComponent', 'NXExtensionPoint', 'NXContribution', 'NXService') ";
        if (fulltext!=null) {
            query= query + " AND ecm:fulltext like '" + fulltext + "'";
        }

        DocumentModelList docs = session.query(query);

        for (DocumentModel doc : docs) {

            NuxeoArtifact artifact = mapDoc2Artifact(doc);
            if (artifact!=null) {
                result.add(artifact);
            }
        }
        return result;
    }

    public List<DocumentationItem> searchDocumentation(CoreSession session,String fulltext, String targetType) throws Exception {

        List<DocumentationItem> result = new ArrayList<DocumentationItem>();

        String query = "select * from NXDocumentation where ecm:fulltext like '" + fulltext + "'";

        if (targetType!=null) {
            query = query + " AND nxdoc.targetType='" + targetType + "'";
        }

        DocumentModelList docs = session.query(query);

        for (DocumentModel doc : docs) {
            DocumentationItem docItem = doc.getAdapter(DocumentationItem.class);
            if (docItem!=null) {
                result.add(docItem);
            }
        }

        return result;
    }

    public List<NuxeoArtifact> filterArtifact(CoreSession session, String distribId, String type, String fulltext) throws Exception {
        List<NuxeoArtifact> result = new ArrayList<NuxeoArtifact>();

        List<NuxeoArtifact> matchingArtifacts = searchArtifact(session, fulltext);
        List<DocumentationItem> matchingDocumentationItems = searchDocumentation(session,fulltext, null);

        Map<String, ArtifactWithWeight> sortMap = new HashMap<String, ArtifactWithWeight>();

        for (NuxeoArtifact matchingArtifact: matchingArtifacts) {
            NuxeoArtifact resultArtifact = resolveInTree(session, distribId, matchingArtifact, type);
            if (resultArtifact!=null) {
                if (sortMap.containsKey(resultArtifact.getId())) {
                    sortMap.get(resultArtifact.getId()).addHit();
                } else {
                    sortMap.put(resultArtifact.getId(), new ArtifactWithWeight(resultArtifact));
                }
            }
        }

        for (DocumentationItem matchingDocumentationItem: matchingDocumentationItems) {
            NuxeoArtifact resultArtifact = resolveInTree(session, distribId, matchingDocumentationItem, type);
            if (resultArtifact!=null) {
                if (sortMap.containsKey(resultArtifact.getId())) {
                    sortMap.get(resultArtifact.getId()).addHit();
                } else {
                    sortMap.put(resultArtifact.getId(), new ArtifactWithWeight(resultArtifact));
                }
            }
        }

        List<ArtifactWithWeight> artifacts = new ArrayList<ArtifactWithWeight>(sortMap.values());
        Collections.sort(artifacts);

        for (ArtifactWithWeight item : artifacts) {
            result.add(item.getArtifact());
        }
        return result;
    }

    protected NuxeoArtifact resolveInTree(CoreSession session, String distribId, NuxeoArtifact matchingArtifact, String searchedType) throws Exception {

        SnapshotManager sm = Framework.getLocalService(SnapshotManager.class);
        DistributionSnapshot snap = sm.getSnapshot(distribId, session);

        String cType = matchingArtifact.getArtifactType();

        if (cType.equals(searchedType)) {
            return matchingArtifact;
        }

        BaseNuxeoArtifactDocAdapter docAdapter = (BaseNuxeoArtifactDocAdapter) matchingArtifact;
        DocumentModel doc = docAdapter.getDoc();

        List<DocumentModel> parents = session.getParentDocuments(doc.getRef());
        Collections.reverse(parents);

        for (DocumentModel parent : parents) {
            if (parent.getType().equals(searchedType)) {
                return mapDoc2Artifact(parent);
            }
        }
        return null;
    }

    protected NuxeoArtifact resolveInTree(CoreSession session, String distribId, DocumentationItem matchingDocumentationItem, String searchedType) throws Exception {

        String targetId = matchingDocumentationItem.getTarget();
        String targetType = matchingDocumentationItem.getTargetType();

        SnapshotManager sm = Framework.getLocalService(SnapshotManager.class);
        DistributionSnapshot snap = sm.getSnapshot(distribId, session);

        NuxeoArtifact artifact = null;

        if (targetType.equals(BundleGroup.TYPE_NAME)) {
            artifact=snap.getBundleGroup(targetId);
        } else if (targetType.equals(BundleInfo.TYPE_NAME)) {
            artifact=snap.getBundle(targetId);
        } else if (targetType.equals(ComponentInfo.TYPE_NAME)) {
            artifact=snap.getComponent(targetId);
        } else if (targetType.equals(ExtensionPointInfo.TYPE_NAME)) {
            artifact=snap.getExtensionPoint(targetId);
        } else  if (targetType.equals(ExtensionInfo.TYPE_NAME)) {
            artifact=snap.getContribution(targetId);
        } else if (targetType.equals(ServiceInfo.TYPE_NAME)) {
            artifact=snap.getService(targetId);
        }

        return resolveInTree(session, distribId, artifact, searchedType);
    }

}
