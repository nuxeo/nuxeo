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

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.adapters.BundleGroupDocAdapter;
import org.nuxeo.apidoc.adapters.BundleInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ComponentInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ExtensionInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ExtensionPointInfoDocAdapter;
import org.nuxeo.apidoc.adapters.OperationInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ServiceInfoDocAdapter;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.documentation.DocumentationItemDocAdapter;
import org.nuxeo.apidoc.documentation.DocumentationService;
import org.nuxeo.apidoc.documentation.ResourceDocumentationItem;
import org.nuxeo.apidoc.introspection.BundleGroupImpl;
import org.nuxeo.apidoc.introspection.BundleInfoImpl;
import org.nuxeo.apidoc.introspection.OperationInfoImpl;
import org.nuxeo.apidoc.plugin.Plugin;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.runtime.api.Framework;

public class SnapshotPersister {

    public static final String Root_PATH = "/";

    public static final String Root_NAME = "nuxeo-distributions";

    public static final String Operation_Root_NAME = "Automation";

    public static final String Bundle_Root_NAME = "Bundles";

    public static final String Read_Grp = "Everyone";

    public static final String Write_Grp = "members";

    protected static final Log log = LogFactory.getLog(SnapshotPersister.class);

    public DocumentModel getSubRoot(CoreSession session, DocumentModel root, String name) {

        DocumentRef rootRef = new PathRef(root.getPathAsString() + name);
        if (session.exists(rootRef)) {
            return session.getDocument(rootRef);
        }
        UnrestrictedRootCreator creator = new UnrestrictedRootCreator(session, root.getPathAsString(), name, false);
        creator.runUnrestricted();
        // flush caches
        session.save();
        return session.getDocument(creator.getRootRef());
    }

    public DocumentModel getDistributionRoot(CoreSession session) {
        DocumentRef rootRef = new PathRef(Root_PATH + Root_NAME);
        if (session.exists(rootRef)) {
            return session.getDocument(rootRef);
        }
        UnrestrictedRootCreator creator = new UnrestrictedRootCreator(session, Root_PATH, Root_NAME, true);
        creator.runUnrestricted();
        // flush caches
        session.save();
        return session.getDocument(creator.getRootRef());
    }

    public DistributionSnapshot persist(DistributionSnapshot snapshot, CoreSession session, String label,
            SnapshotFilter filter, Map<String, Serializable> properties, List<Plugin<?>> plugins) {

        RepositoryDistributionSnapshot distribContainer = createDistributionDoc(snapshot, session, label, properties);

        if (filter == null) {
            // If no filter, clean old entries
            distribContainer.cleanPreviousArtifacts();
        }

        DocumentModel bundleContainer = getSubRoot(session, distribContainer.getDoc(), Bundle_Root_NAME);

        if (filter != null) {
            // create VGroup that contain,s only the target bundles
            BundleGroupImpl vGroup = new BundleGroupImpl(filter.getBundleGroupName(), snapshot.getVersion());
            for (String bundleId : snapshot.getBundleIds()) {
                if (filter.includeBundleId(bundleId)) {
                    vGroup.add(bundleId);
                }
            }
            persistBundleGroup(snapshot, vGroup, session, label + "-bundles", bundleContainer);
        } else {
            List<BundleGroup> bundleGroups = snapshot.getBundleGroups();
            for (BundleGroup bundleGroup : bundleGroups) {
                persistBundleGroup(snapshot, bundleGroup, session, label, bundleContainer);
            }
        }

        DocumentModel opContainer = getSubRoot(session, distribContainer.getDoc(), Operation_Root_NAME);
        persistOperations(snapshot, snapshot.getOperations(), session, label, opContainer, filter);

        // handle plugins persistence
        for (Plugin<?> plugin : plugins) {
            plugin.persist(snapshot, session, distribContainer.getDoc(), filter);
        }

        // needed for tests
        session.save();

        return distribContainer;
    }

    public void persistOperations(DistributionSnapshot snapshot, List<OperationInfo> operations, CoreSession session,
            String label, DocumentModel parent, SnapshotFilter filter) {
        for (OperationInfo op : operations) {
            if (filter == null || op instanceof OperationInfoImpl && filter.includeOperation((OperationInfoImpl) op)) {
                persistOperation(snapshot, op, session, label, parent);
            }
        }
    }

    public void persistOperation(DistributionSnapshot snapshot, OperationInfo op, CoreSession session, String label,
            DocumentModel parent) {
        OperationInfoDocAdapter.create(op, session, parent.getPathAsString());
    }

    public void persistBundleGroup(DistributionSnapshot snapshot, BundleGroup bundleGroup, CoreSession session,
            String label, DocumentModel parent) {
        if (log.isTraceEnabled()) {
            log.trace("Persist bundle group " + bundleGroup.getId());
        }

        DocumentModel bundleGroupDoc = createBundleGroupDoc(bundleGroup, session, label, parent);

        // save GitHub doc
        if (bundleGroup instanceof BundleGroupImpl) {
            Map<String, ResourceDocumentationItem> liveDoc = ((BundleGroupImpl) bundleGroup).getLiveDoc();
            if (liveDoc != null && liveDoc.size() > 0) {
                persistLiveDoc(liveDoc, bundleGroupDoc.getAdapter(BundleGroup.class), session);
            }
        }

        for (String bundleId : bundleGroup.getBundleIds()) {
            BundleInfo bi = snapshot.getBundle(bundleId);
            persistBundle(snapshot, bi, session, label, bundleGroupDoc);
        }

        for (BundleGroup subGroup : bundleGroup.getSubGroups()) {
            persistBundleGroup(snapshot, subGroup, session, label, bundleGroupDoc);
        }
    }

    protected void persistLiveDoc(Map<String, ResourceDocumentationItem> liveDoc, NuxeoArtifact item,
            CoreSession session) {
        DocumentationService ds = Framework.getService(DocumentationService.class);
        List<DocumentationItem> existingDocs = ds.findDocumentItems(session, item);
        for (String cat : liveDoc.keySet()) {
            ResourceDocumentationItem docItem = liveDoc.get(cat);
            DocumentationItem previousDocItem = null;
            for (DocumentationItem exiting : existingDocs) {
                if (exiting.getTitle().equals(docItem.getTitle()) && exiting.getTarget().equals(docItem.getTarget())) {
                    previousDocItem = exiting;
                    break;
                }
            }
            if (previousDocItem == null) {
                ds.createDocumentationItem(session, item, docItem.getTitle(), docItem.getContent(), cat,
                        Arrays.asList(item.getVersion()), docItem.isApproved(), docItem.getRenderingType());
            } else {
                if (previousDocItem instanceof DocumentationItemDocAdapter) {
                    DocumentationItemDocAdapter existingDoc = (DocumentationItemDocAdapter) previousDocItem;
                    Blob blob = Blobs.createBlob(docItem.getContent());
                    Blob oldBlob = (Blob) existingDoc.getDocumentModel()
                                                     .getPropertyValue(NuxeoArtifact.CONTENT_PROPERTY_PATH);
                    blob.setFilename(oldBlob.getFilename());
                    existingDoc.getDocumentModel()
                               .setPropertyValue(NuxeoArtifact.CONTENT_PROPERTY_PATH, (Serializable) blob);
                    ds.updateDocumentationItem(session, existingDoc);
                }

            }
        }
    }

    public void persistBundle(DistributionSnapshot snapshot, BundleInfo bundleInfo, CoreSession session, String label,
            DocumentModel parent) {
        if (log.isTraceEnabled()) {
            log.trace("Persist bundle " + bundleInfo.getId());
        }
        DocumentModel bundleDoc = createBundleDoc(snapshot, session, label, bundleInfo, parent);

        // save GitHub doc
        if (bundleInfo instanceof BundleInfoImpl) {
            Map<String, ResourceDocumentationItem> liveDoc = ((BundleInfoImpl) bundleInfo).getLiveDoc();
            if (liveDoc != null && liveDoc.size() > 0) {
                persistLiveDoc(liveDoc, bundleDoc.getAdapter(BundleInfo.class), session);
            }
        }

        for (ComponentInfo ci : bundleInfo.getComponents()) {
            persistComponent(snapshot, ci, session, label, bundleDoc);
        }
    }

    public void persistComponent(DistributionSnapshot snapshot, ComponentInfo ci, CoreSession session, String label,
            DocumentModel parent) {

        DocumentModel componentDoc = createComponentDoc(snapshot, session, label, ci, parent);

        for (ExtensionPointInfo epi : ci.getExtensionPoints()) {
            createExtensionPointDoc(snapshot, session, label, epi, componentDoc);
        }
        Map<String, AtomicInteger> comps = new HashMap<>();
        for (ExtensionInfo ei : ci.getExtensions()) {
            // handle multiple contributions to the same extension point
            String id = ei.getId();
            comps.computeIfAbsent(id, k -> new AtomicInteger(-1)).incrementAndGet();
            createContributionDoc(snapshot, session, label, ei, comps.get(id).get(), componentDoc);
        }

        for (ServiceInfo si : ci.getServices()) {
            createServiceDoc(snapshot, session, label, si, componentDoc);
        }
    }

    protected DocumentModel createContributionDoc(DistributionSnapshot snapshot, CoreSession session, String label,
            ExtensionInfo ei, int index, DocumentModel parent) {
        return ExtensionInfoDocAdapter.create(ei, index, session, parent.getPathAsString()).getDoc();
    }

    protected DocumentModel createServiceDoc(DistributionSnapshot snapshot, CoreSession session, String label,
            ServiceInfo si, DocumentModel parent) {
        return ServiceInfoDocAdapter.create(si, session, parent.getPathAsString()).getDoc();
    }

    protected DocumentModel createExtensionPointDoc(DistributionSnapshot snapshot, CoreSession session, String label,
            ExtensionPointInfo epi, DocumentModel parent) {
        return ExtensionPointInfoDocAdapter.create(epi, session, parent.getPathAsString()).getDoc();
    }

    protected DocumentModel createComponentDoc(DistributionSnapshot snapshot, CoreSession session, String label,
            ComponentInfo ci, DocumentModel parent) {
        try {
            return ComponentInfoDocAdapter.create(ci, session, parent.getPathAsString()).getDoc();
        } catch (IOException e) {
            throw new NuxeoException("Unable to create Component Doc", e);
        }
    }

    protected DocumentModel createBundleDoc(DistributionSnapshot snapshot, CoreSession session, String label,
            BundleInfo bi, DocumentModel parent) {
        return BundleInfoDocAdapter.create(bi, session, parent.getPathAsString()).getDoc();
    }

    protected RepositoryDistributionSnapshot createDistributionDoc(DistributionSnapshot snapshot, CoreSession session,
            String label, Map<String, Serializable> properties) {
        return RepositoryDistributionSnapshot.create(snapshot, session, getDistributionRoot(session).getPathAsString(),
                label, properties);
    }

    protected DocumentModel createBundleGroupDoc(BundleGroup bundleGroup, CoreSession session, String label,
            DocumentModel parent) {
        return BundleGroupDocAdapter.create(bundleGroup, session, parent.getPathAsString()).getDoc();
    }

}
