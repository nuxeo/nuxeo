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
package org.nuxeo.apidoc.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.text.StrBuilder;
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
import org.nuxeo.apidoc.api.QueryHelper;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.repository.RepositoryDistributionSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;

public class ArtifactSearcherImpl implements ArtifactSearcher {

    protected static final int MAX_RESULTS = 1000;

    protected NuxeoArtifact mapDoc2Artifact(DocumentModel doc) {
        NuxeoArtifact artifact = null;

        if (doc.getType().equals(BundleGroup.TYPE_NAME)) {
            artifact = new BundleGroupDocAdapter(doc);
        } else if (doc.getType().equals(BundleInfo.TYPE_NAME)) {
            artifact = new BundleInfoDocAdapter(doc);
        } else if (doc.getType().equals(ComponentInfo.TYPE_NAME)) {
            artifact = new ComponentInfoDocAdapter(doc);
        } else if (doc.getType().equals(ExtensionPointInfo.TYPE_NAME)) {
            artifact = new ExtensionPointInfoDocAdapter(doc);
        } else if (doc.getType().equals(ExtensionInfo.TYPE_NAME)) {
            artifact = new ExtensionInfoDocAdapter(doc);
        } else if (doc.getType().equals(DistributionSnapshot.TYPE_NAME)) {
            artifact = new RepositoryDistributionSnapshot(doc);
        } else if (doc.getType().equals(ServiceInfo.TYPE_NAME)) {
            artifact = new ServiceInfoDocAdapter(doc);
        }

        return artifact;
    }

    @Override
    public List<NuxeoArtifact> searchArtifact(CoreSession session, String distribId, String fulltext) {
        List<NuxeoArtifact> result = new ArrayList<>();

        DistributionSnapshot snap = Framework.getService(SnapshotManager.class).getSnapshot(distribId, session);
        if (!(snap instanceof RepositoryDistributionSnapshot)) {
            return Collections.emptyList();
        }

        DocumentModel dist = ((RepositoryDistributionSnapshot) snap).getDoc();
        StrBuilder q = new StrBuilder("SELECT * FROM Document WHERE ");
        q.append("ecm:path STARTSWITH '").append(dist.getPathAsString()).append("'");
        String query = q.toString();
        if (fulltext != null) {
            query += " AND " + NXQL.ECM_FULLTEXT + " = " + NXQL.escapeString(fulltext);
        }

        ElasticSearchService ess = Framework.getService(ElasticSearchService.class);
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql(query).limit(MAX_RESULTS));
        for (DocumentModel doc : docs) {
            NuxeoArtifact artifact = mapDoc2Artifact(doc);
            if (artifact != null) {
                result.add(artifact);
            }
        }
        return result;
    }

    @Override
    public List<DocumentationItem> searchDocumentation(CoreSession session, String distribId, String fulltext,
            String targetType) {
        DistributionSnapshot snap = Framework.getService(SnapshotManager.class).getSnapshot(distribId, session);
        DocumentModel dist = ((RepositoryDistributionSnapshot) snap).getDoc();
        String query = QueryHelper.select(DocumentationItem.TYPE_NAME, dist, NXQL.ECM_FULLTEXT, fulltext);
        if (targetType != null) {
            query += " AND " + DocumentationItem.PROP_TARGET_TYPE + " = " + NXQL.escapeString(targetType);
        }

        ElasticSearchService ess = Framework.getService(ElasticSearchService.class);
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql(query).limit(MAX_RESULTS));
        List<DocumentationItem> result = new ArrayList<>();
        for (DocumentModel doc : docs) {
            DocumentationItem docItem = doc.getAdapter(DocumentationItem.class);
            if (docItem != null) {
                result.add(docItem);
            }
        }
        return result;
    }

    @Override
    public List<NuxeoArtifact> filterArtifact(CoreSession session, String distribId, String type, String fulltext) {
        List<NuxeoArtifact> result = new ArrayList<>();

        List<NuxeoArtifact> matchingArtifacts = searchArtifact(session, distribId, fulltext);
        List<DocumentationItem> matchingDocumentationItems = searchDocumentation(session, distribId, fulltext, null);

        Map<String, ArtifactWithWeight> sortMap = new HashMap<>();

        for (NuxeoArtifact matchingArtifact : matchingArtifacts) {
            ArtifactWithWeight artifactWithWeight;
            NuxeoArtifact matchingParentArtifact = resolveInTree(session, distribId, matchingArtifact, type);
            if (matchingParentArtifact != null) {
                artifactWithWeight = new ArtifactWithWeight(matchingParentArtifact);
            } else if (matchingArtifact.getArtifactType().equals(type)) {
                artifactWithWeight = new ArtifactWithWeight(matchingArtifact);
            } else {
                continue;
            }

            String id = artifactWithWeight.getArtifact().getId();
            if (sortMap.containsKey(id)) {
                sortMap.get(id).addHit();
            } else {
                sortMap.put(id, new ArtifactWithWeight(matchingParentArtifact));
            }
        }

        for (DocumentationItem matchingDocumentationItem : matchingDocumentationItems) {
            NuxeoArtifact resultArtifact = resolveInTree(session, distribId, matchingDocumentationItem, type);
            if (resultArtifact != null) {
                if (sortMap.containsKey(resultArtifact.getId())) {
                    sortMap.get(resultArtifact.getId()).addHit();
                } else {
                    sortMap.put(resultArtifact.getId(), new ArtifactWithWeight(resultArtifact));
                }
            }
        }

        List<ArtifactWithWeight> artifacts = new ArrayList<>(sortMap.values());
        Collections.sort(artifacts);

        for (ArtifactWithWeight item : artifacts) {
            result.add(item.getArtifact());
        }
        return result;
    }

    protected NuxeoArtifact resolveInTree(CoreSession session, String distribId, NuxeoArtifact matchingArtifact,
            String searchedType) {
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

    protected NuxeoArtifact resolveInTree(CoreSession session, String distribId,
            DocumentationItem matchingDocumentationItem, String searchedType) {
        DistributionSnapshot snap = Framework.getService(SnapshotManager.class).getSnapshot(distribId, session);
        String targetId = matchingDocumentationItem.getTarget();
        String targetType = matchingDocumentationItem.getTargetType();
        NuxeoArtifact artifact;
        if (targetType.equals(BundleGroup.TYPE_NAME)) {
            artifact = snap.getBundleGroup(targetId);
        } else if (targetType.equals(BundleInfo.TYPE_NAME)) {
            artifact = snap.getBundle(targetId);
        } else if (targetType.equals(ComponentInfo.TYPE_NAME)) {
            artifact = snap.getComponent(targetId);
        } else if (targetType.equals(ExtensionPointInfo.TYPE_NAME)) {
            artifact = snap.getExtensionPoint(targetId);
        } else if (targetType.equals(ExtensionInfo.TYPE_NAME)) {
            artifact = snap.getContribution(targetId);
        } else if (targetType.equals(ServiceInfo.TYPE_NAME)) {
            artifact = snap.getService(targetId);
        } else {
            artifact = null;
        }
        return resolveInTree(session, distribId, artifact, searchedType);
    }

}
