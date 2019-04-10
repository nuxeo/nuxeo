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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.adapters.BundleGroupDocAdapter;
import org.nuxeo.apidoc.adapters.BundleInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ComponentInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ExtensionInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ExtensionPointInfoDocAdapter;
import org.nuxeo.apidoc.adapters.OperationInfoDocAdapter;
import org.nuxeo.apidoc.adapters.SeamComponentInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ServiceInfoDocAdapter;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.SeamComponentInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.introspection.BundleGroupImpl;
import org.nuxeo.apidoc.introspection.OperationInfoImpl;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;

public class SnapshotPersister {

    public static final String Root_PATH = "/";

    public static final String Root_NAME = "nuxeo-distributions";

    public static final String Read_Grp = "Everyone";

    public static final String Write_Grp = "members";

    protected static final Log log = LogFactory.getLog(SnapshotPersister.class);

    class UnrestrictedRootCreator extends UnrestrictedSessionRunner {

        protected DocumentRef rootRef;

        public UnrestrictedRootCreator(CoreSession session) {
            super(session);
        }

        public DocumentRef getRootRef() {
            return rootRef;
        }

        @Override
        public void run() throws ClientException {

            DocumentModel root = session.createDocumentModel(Root_PATH,
                    Root_NAME, "Workspace");
            root.setProperty("dublincore", "title", Root_NAME);
            root = session.createDocument(root);

            ACL acl = new ACLImpl();
            acl.add(new ACE(Write_Grp, "Write", true));
            acl.add(new ACE(Read_Grp, "Read", true));
            ACP acp = root.getACP();
            acp.addACL(acl);
            session.setACP(root.getRef(), acp, true);

            rootRef = root.getRef();
            // flush caches
            session.save();
        }

    }

    public DocumentModel getDistributionRoot(CoreSession session)
            throws ClientException {
        DocumentRef rootRef = new PathRef(Root_PATH + Root_NAME);

        if (session.exists(rootRef)) {
            return session.getDocument(rootRef);
        }

        UnrestrictedRootCreator creator = new UnrestrictedRootCreator(session);

        creator.runUnrestricted();

        // flush caches
        session.save();
        return session.getDocument(creator.getRootRef());
    }

    public DistributionSnapshot persist(DistributionSnapshot snapshot,
            CoreSession session, String label, SnapshotFilter filter) throws ClientException {
        if (label == null || "".equals(label.trim())) {
            label = snapshot.getName() + "-" + snapshot.getVersion();
        }

        RepositoryDistributionSnapshot distribContainer = createDistributionDoc(
                snapshot, session, label);

        if (filter!=null) {
            // create VGroup that contain,s only the target bundles
            BundleGroupImpl vGroup = new BundleGroupImpl(filter.getBundleGroupName(), snapshot.getVersion());
            for (String bundleId : snapshot.getBundleIds()) {
                if (filter.includeBundleId(bundleId)) {
                    vGroup.add(bundleId);
                }
            }
            persistBundleGroup(snapshot, vGroup, session, label + "-bundles",
                    distribContainer.getDoc());
        } else {
            List<BundleGroup> bundleGroups = snapshot.getBundleGroups();
            for (BundleGroup bundleGroup : bundleGroups) {
                persistBundleGroup(snapshot, bundleGroup, session, label,
                        distribContainer.getDoc());
            }
        }
        persistSeamComponents(snapshot, snapshot.getSeamComponents(), session,
                label, distribContainer.getDoc(), filter);

        persistOperations(snapshot, snapshot.getOperations(), session, label,
                distribContainer.getDoc(), filter);

        return distribContainer;
    }

    public void persistSeamComponents(DistributionSnapshot snapshot,
            List<SeamComponentInfo> seamComponents, CoreSession session,
            String label, DocumentModel parent, SnapshotFilter filter) throws ClientException {
        for (SeamComponentInfo seamComponent : seamComponents) {
            if (filter==null || filter.includeSeamComponent(seamComponent)) {
                persistSeamComponent(snapshot, seamComponent, session, label,
                    parent);
            }
        }
    }

    public void persistSeamComponent(DistributionSnapshot snapshot,
            SeamComponentInfo seamComponent, CoreSession session, String label,
            DocumentModel parent) throws ClientException {
        try {
            SeamComponentInfoDocAdapter.create(seamComponent, session,
                    parent.getPathAsString());
        } catch (Exception e) {
            throw new ClientException(
                    "Errors while persisting Seam Component as document", e);
        }
    }

    public void persistOperations(DistributionSnapshot snapshot,
            List<OperationInfo> operations, CoreSession session, String label,
            DocumentModel parent,SnapshotFilter filter) throws ClientException {
        for (OperationInfo op : operations) {
            if (filter==null || (op instanceof OperationInfoImpl && filter.includeOperation((OperationInfoImpl) op))) {
                persistOperation(snapshot, op, session, label, parent);
            }
        }
    }

    public void persistOperation(DistributionSnapshot snapshot,
            OperationInfo op, CoreSession session, String label,
            DocumentModel parent) throws ClientException {
        try {
            OperationInfoDocAdapter.create(op, session,
                    parent.getPathAsString());
        } catch (Exception e) {
            throw new ClientException(
                    "Errors while persisting Operation as document", e);
        }
    }

    public void persistBundleGroup(DistributionSnapshot snapshot,
            BundleGroup bundleGroup, CoreSession session, String label,
            DocumentModel parent) throws ClientException {
        log.info("Persist bundle group " + bundleGroup.getId());

        DocumentModel bundleGroupDoc = createBundleGroupDoc(bundleGroup,
                session, label, parent);

        for (String bundleId : bundleGroup.getBundleIds()) {
            BundleInfo bi = snapshot.getBundle(bundleId);
            persistBundle(snapshot, bi, session, label, bundleGroupDoc);
        }

        for (BundleGroup subGroup : bundleGroup.getSubGroups()) {
            persistBundleGroup(snapshot, subGroup, session, label,
                    bundleGroupDoc);
        }
    }

    public void persistBundle(DistributionSnapshot snapshot,
            BundleInfo bundleInfo, CoreSession session, String label,
            DocumentModel parent) throws ClientException {
        log.info("Persist bundle " + bundleInfo.getId());

        DocumentModel bundleDoc = createBundleDoc(snapshot, session, label,
                bundleInfo, parent);

        for (ComponentInfo ci : bundleInfo.getComponents()) {
            persistComponent(snapshot, ci, session, label, bundleDoc);
        }
    }

    public void persistComponent(DistributionSnapshot snapshot,
            ComponentInfo ci, CoreSession session, String label,
            DocumentModel parent) throws ClientException {

        DocumentModel componentDoc = createComponentDoc(snapshot, session,
                label, ci, parent);

        for (ExtensionPointInfo epi : ci.getExtensionPoints()) {
            createExtensionPointDoc(snapshot, session, label, epi, componentDoc);
        }
        for (ExtensionInfo ei : ci.getExtensions()) {
            createContributionDoc(snapshot, session, label, ei, componentDoc);
        }

        for (ServiceInfo si : ci.getServices()) {
            createServiceDoc(snapshot, session, label, si, componentDoc);
        }
    }

    protected DocumentModel createContributionDoc(
            DistributionSnapshot snapshot, CoreSession session, String label,
            ExtensionInfo ei, DocumentModel parent) throws ClientException {
        try {
            return ExtensionInfoDocAdapter.create(ei, session,
                    parent.getPathAsString()).getDoc();
        } catch (Exception e) {
            throw new ClientException("Unable to create Contribution Document",
                    e);
        }
    }

    protected DocumentModel createServiceDoc(DistributionSnapshot snapshot,
            CoreSession session, String label, ServiceInfo si,
            DocumentModel parent) throws ClientException {
        try {
            return ServiceInfoDocAdapter.create(si, session,
                    parent.getPathAsString()).getDoc();
        } catch (Exception e) {
            throw new ClientException("Unable to create Contribution Document",
                    e);
        }
    }

    protected DocumentModel createExtensionPointDoc(
            DistributionSnapshot snapshot, CoreSession session, String label,
            ExtensionPointInfo epi, DocumentModel parent)
            throws ClientException {

        try {
            return ExtensionPointInfoDocAdapter.create(epi, session,
                    parent.getPathAsString()).getDoc();
        } catch (Exception e) {
            throw new ClientException(
                    "Unable to create ExtensionPoint Document", e);
        }
    }

    protected DocumentModel createComponentDoc(DistributionSnapshot snapshot,
            CoreSession session, String label, ComponentInfo ci,
            DocumentModel parent) throws ClientException {
        try {
            return ComponentInfoDocAdapter.create(ci, session,
                    parent.getPathAsString()).getDoc();
        } catch (Exception e) {
            throw new ClientException("Unable to create Component Doc", e);
        }
    }

    protected DocumentModel createBundleDoc(DistributionSnapshot snapshot,
            CoreSession session, String label, BundleInfo bi,
            DocumentModel parent) throws ClientException {
        return BundleInfoDocAdapter.create(bi, session,
                parent.getPathAsString()).getDoc();
    }

    protected RepositoryDistributionSnapshot createDistributionDoc(
            DistributionSnapshot snapshot, CoreSession session, String label)
            throws ClientException {
        return RepositoryDistributionSnapshot.create(snapshot, session,
                getDistributionRoot(session).getPathAsString(), label);
    }

    protected DocumentModel createBundleGroupDoc(BundleGroup bundleGroup,
            CoreSession session, String label, DocumentModel parent)
            throws ClientException {
        return BundleGroupDocAdapter.create(bundleGroup, session,
                parent.getPathAsString()).getDoc();
    }

}
