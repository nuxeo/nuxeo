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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.adapters.BundleGroupDocAdapter;
import org.nuxeo.apidoc.adapters.BundleInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ComponentInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ExtensionInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ExtensionPointInfoDocAdapter;
import org.nuxeo.apidoc.adapters.ServiceInfoDocAdapter;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
public class SnapshotPersister {

    protected static Log log = LogFactory.getLog(SnapshotPersister.class);

    public DistributionSnapshot persist(DistributionSnapshot snapshot, CoreSession session, String label) throws ClientException {

        if (label==null)  {
            label = snapshot.getName() + "-" + snapshot.getVersion();
        }

        RepositoryDistributionSnapshot distribContainer = createDistributionDoc(snapshot, session, label);

        List<BundleGroup> bundleGroups = snapshot.getBundleGroups();
        for (BundleGroup bundleGroup : bundleGroups) {
            persistBundleGroup(snapshot,bundleGroup, session, label, distribContainer.getDoc());
        }

        return distribContainer;
    }


    public void persistBundleGroup(DistributionSnapshot snapshot, BundleGroup bundleGroup, CoreSession session, String label,  DocumentModel parent) throws ClientException {

       log.info("Persit bundle group " + bundleGroup.getId());
       DocumentModel bundleGroupDoc = createBundleGroupDoc(bundleGroup, session, label, parent);

       for (String bundleId : bundleGroup.getBundleIds()) {
           BundleInfo bi = snapshot.getBundle(bundleId);
           persistBundle(snapshot, bi, session, label, bundleGroupDoc);
       }

       for (BundleGroup subGroup : bundleGroup.getSubGroups()) {
           persistBundleGroup(snapshot, subGroup, session, label, bundleGroupDoc);
       }

    }


    public void persistBundle(DistributionSnapshot snapshot, BundleInfo bundleInfo, CoreSession session, String label, DocumentModel parent) throws ClientException {

        log.info("Persit bundle " + bundleInfo.getId());

        DocumentModel bundleDoc = createBundleDoc(snapshot, session, label, bundleInfo, parent);

        for (ComponentInfo ci : bundleInfo.getComponents()) {
            persistComponent(snapshot, ci, session, label, bundleDoc);
        }

    }

    public void persistComponent(DistributionSnapshot snapshot, ComponentInfo ci, CoreSession session, String label, DocumentModel parent) throws ClientException {

        DocumentModel componentDoc = createComponentDoc(snapshot, session, label, ci, parent);

        for (ExtensionPointInfo epi : ci.getExtensionPoints()) {
            createExtensionPointDoc(snapshot, session, label, epi, componentDoc);
        }
        for (ExtensionInfo ei : ci.getExtensions() ) {
            createContributionDoc(snapshot, session, label, ei, componentDoc);
        }

        for (ServiceInfo si : ci.getServices()) {
            createServiceDoc(snapshot, session, label, si, componentDoc);
        }
    }

    protected DocumentModel createContributionDoc(DistributionSnapshot snapshot, CoreSession session, String label, ExtensionInfo ei, DocumentModel parent) throws ClientException{
        try {
            return ExtensionInfoDocAdapter.create(ei, session, parent.getPathAsString()).getDoc();
        }
        catch (Exception e) {
            throw new ClientException("Unable to create Contribution Document", e);
        }
    }

    protected DocumentModel createServiceDoc(DistributionSnapshot snapshot, CoreSession session, String label, ServiceInfo si, DocumentModel parent) throws ClientException{
        try {
            return ServiceInfoDocAdapter.create(si, session, parent.getPathAsString()).getDoc();
        }
        catch (Exception e) {
            throw new ClientException("Unable to create Contribution Document", e);
        }
    }

    protected DocumentModel createExtensionPointDoc(DistributionSnapshot snapshot, CoreSession session, String label, ExtensionPointInfo epi, DocumentModel parent) throws ClientException{
        try {
            return ExtensionPointInfoDocAdapter.create(epi, session, parent.getPathAsString()).getDoc();
        } catch (Exception e) {
            throw new ClientException("Unable to create ExtensionPoint Document",e);
        }
    }

    protected DocumentModel createComponentDoc(DistributionSnapshot snapshot, CoreSession session, String label, ComponentInfo ci, DocumentModel parent) throws ClientException{
        try {
            return ComponentInfoDocAdapter.create(ci, session, parent.getPathAsString()).getDoc();
        } catch (Exception e) {
            throw new ClientException("Unable to create Component Doc", e);
        }
    }

    protected DocumentModel createBundleDoc(DistributionSnapshot snapshot, CoreSession session, String label, BundleInfo bi, DocumentModel parent) throws ClientException{
        return BundleInfoDocAdapter.create(bi, session, parent.getPathAsString()).getDoc();
    }

    protected RepositoryDistributionSnapshot createDistributionDoc(DistributionSnapshot snapshot, CoreSession session, String label) throws ClientException{
        return RepositoryDistributionSnapshot.create(snapshot, session, "/");
    }

    protected DocumentModel createBundleGroupDoc(BundleGroup bundleGroup, CoreSession session, String label, DocumentModel parent ) throws ClientException{
        return BundleGroupDocAdapter.create(bundleGroup, session, parent.getPathAsString()).getDoc();
    }

}
