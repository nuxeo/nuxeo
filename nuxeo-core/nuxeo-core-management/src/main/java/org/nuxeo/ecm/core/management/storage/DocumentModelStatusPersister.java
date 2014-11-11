/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mcedica
 */
package org.nuxeo.ecm.core.management.storage;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.management.api.AdministrativeStatus;
import org.nuxeo.runtime.api.Framework;

/**
 * Used to control the server administrative status: the status of the server
 * can be passive or active.
 *
 * @author Mariana Cedica
 */
public class DocumentModelStatusPersister implements AdministrativeStatusPersister {

    public static final String ADMINISTRATIVE_INFO_CONTAINER = "administrative-infos";

    public static final String ADMINISTRATIVE_INFO_CONTAINER_DOCUMENT_TYPE = "AdministrativeStatusContainer";

    public static final String ADMINISTRATIVE_STATUS_DOCUMENT_TYPE = "AdministrativeStatus";

    public static final String STATUS_PROPERTY = "status:administrative_status";

    public static final String MESSAGE_PROPERTY = "status:statusMessage";

    public static final String INSTANCE_PROPERTY = "status:instanceId";

    public static final String SERVICE_PROPERTY = "status:serviceId";

    public static final String LOGIN_PROPERTY = "status:userLogin";

    private static final Log log = LogFactory.getLog(DocumentModelStatusPersister.class);

    protected String repositoryName;

    protected String getRepositoryName() {
        if (repositoryName == null) {
            repositoryName = Framework.getLocalService(RepositoryManager.class).getDefaultRepository().getName();
        }
        return repositoryName;
    }


    private class StatusSaver extends UnrestrictedSessionRunner {

        protected AdministrativeStatus status;

        public StatusSaver(String repoName, AdministrativeStatus status) {
            super(repoName);
            this.status = status;
        }

        @Override
        public void run() throws ClientException {
            DocumentModel doc = doGetOrCreateDoc(status);
            session.save();
        }

        public AdministrativeStatus getStatus() {
            return status;
        }

        protected DocumentModel doGetOrCreateContainer() throws ClientException {

            DocumentRef admRootDocRef = new PathRef("/" + ADMINISTRATIVE_INFO_CONTAINER);

            if (!session.exists(admRootDocRef)) {
                DocumentModel doc = session.createDocumentModel("/",
                        ADMINISTRATIVE_INFO_CONTAINER,
                        ADMINISTRATIVE_INFO_CONTAINER_DOCUMENT_TYPE);
                doc.setPropertyValue("dc:title", ADMINISTRATIVE_INFO_CONTAINER);
                doc = session.createDocument(doc);
                session.save();
            }

            return session.getDocument(admRootDocRef);
        }

        protected DocumentModel doGetOrCreateDoc(AdministrativeStatus status) throws ClientException {
            DocumentModel administrativeContainer = doGetOrCreateContainer();

            DocumentRef statusDocRef = new PathRef(
                    administrativeContainer.getPathAsString() + "/"
                    + getAdministrativeStatusDocName(status));

            DocumentModel doc = null;
            boolean create=false;
            if (!session.exists(statusDocRef)) {
                create=true;
                doc = session.createDocumentModel(
                        administrativeContainer.getPathAsString(),
                        getAdministrativeStatusDocName(status),
                        ADMINISTRATIVE_STATUS_DOCUMENT_TYPE);
            } else {
                doc = session.getDocument(statusDocRef);
            }

            doc.setPropertyValue(DocumentModelStatusPersister.LOGIN_PROPERTY, status.getUserLogin());
            doc.setPropertyValue(DocumentModelStatusPersister.INSTANCE_PROPERTY, status.getInstanceIdentifier());
            doc.setPropertyValue(DocumentModelStatusPersister.SERVICE_PROPERTY, status.getServiceIdentifier());
            doc.setPropertyValue(DocumentModelStatusPersister.MESSAGE_PROPERTY, status.getMessage());
            doc.setPropertyValue(DocumentModelStatusPersister.STATUS_PROPERTY, status.getState());

            doc.setPropertyValue("dc:title", getAdministrativeStatusDocName(status));

            if (create) {
                doc = session.createDocument(doc);
            }
            else {
                doc = session.saveDocument(doc);
            }
            session.save();

            return doc;
        }

    }


    protected String getAdministrativeStatusDocName(AdministrativeStatus status) {
        return status.getInstanceIdentifier() + "--" + status.getServiceIdentifier();
    }

    @Override
    public List<String> getAllInstanceIds() {
        StatusFetcher fetcher = new StatusFetcher(getRepositoryName(), null, null);
        try {
            fetcher.runUnrestricted();
            return fetcher.allInstanceIds;
        }
        catch (ClientException e) {
            log.error("Error while fetching all instance Ids", e);
            return null;
        }
    }


    @Override
    public List<AdministrativeStatus> getAllStatuses(String instanceId) {
        StatusFetcher fetcher = new StatusFetcher(getRepositoryName(), instanceId, null);
        try {
            fetcher.runUnrestricted();
            return fetcher.statuses;
        }
        catch (ClientException e) {
            log.error("Error while fetching all service status for instance " + instanceId , e);
            return null;
        }
    }


    @Override
    public AdministrativeStatus getStatus(String instanceId,
            String serviceIdentifier) {
        StatusFetcher fetcher = new StatusFetcher(getRepositoryName(), instanceId, serviceIdentifier);
        try {
            fetcher.runUnrestricted();
            if (fetcher.statuses.size()==1) {
                return fetcher.statuses.get(0);
            } else {
                log.warn("Unable to fetch status for service " + serviceIdentifier + " in instance " + instanceId);
                return null;
            }
        }
        catch (ClientException e) {
            log.error("Error while fetching all service status for instance " + instanceId , e);
            return null;
        }
    }


    @Override
    public void remove(String instanceId) {
        throw new UnsupportedOperationException("Not implemented for now");
    }


    @Override
    public AdministrativeStatus saveStatus(AdministrativeStatus status) {

        try {
            StatusSaver saver = new StatusSaver(getRepositoryName(), status);
            saver.runUnrestricted();
            return saver.getStatus();
        } catch (Exception e) {
            log.error("Error while saving status", e);
            return null;
        }

    }


}
