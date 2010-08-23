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
package org.nuxeo.ecm.platform.management.statuses;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.runtime.api.Framework;

/**
 * Used to control the server administrative status: the status of the server
 * can be passive or active.
 * 
 * @author Mariana Cedica
 */
public class NuxeoAdministrativeStatusPersister implements AdministrativeStatusPersister {

    public static final String ADMINISTRATIVE_INFO_CONTAINER = "administrative-infos";

    public static final String ADMINISTRATIVE_STATUS_DOCUMENT = "administrative-status";

    public static final String ADMINISTRATIVE_STATUS_DOCUMENT_TYPE = "AdministrativeStatus";

    public static final String ADMINISTRATIVE_STATUS_PROPERTY = "status:administrative_status";

	private static final Log log = LogFactory.getLog(NuxeoAdministrativeStatusPersister.class);

	protected String repositoryName;
	
	public String setValue(String serverInstanceName, String newState) {
		Setter setter = new Setter(getRepositoryName(), serverInstanceName, newState);
		try {
			setter.runUnrestricted();
		} catch (ClientException e) {
			throw new ClientRuntimeException(e);
		}
		return setter.lastState;
	}
	

    public String getValue(String serverInstanceName) {
        try {
            return getStatusValueFromDocument(serverInstanceName);
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected String getStatusValueFromDocument(String serverInstanceName)
            throws ClientException {
        Fetcher fetcher = new Fetcher(getRepositoryName(), serverInstanceName);
        try {
            fetcher.runUnrestricted();
        } catch (ClientException e) {
            log.error("Unable to fetch the administrative status document", e);
            throw new ClientException(e);
        }
        return (String) fetcher.getAdministrativeStatusPropertyValue();
    }

	protected DocumentModel getOrCreateStatusDocument(String serverInstanceName) throws ClientException {
		Fetcher fetcher = new Fetcher(
		        getRepositoryName(), serverInstanceName);
		try {
			fetcher.runUnrestricted();
		} catch (ClientException e) {
			log.error("Unable to fetch the administrative status document", e);
			throw new ClientException(e);
		}
		return fetcher.getDocument();
	}


	private class Setter extends
			UnrestrictedSessionRunner {

		protected String serverInstanceName;
		protected String newState;
		protected String lastState;

		public Setter(String repoName,
				String serverInstanceName, String state) {
			super(repoName);
			this.newState = state;
		}

		@Override
		public void run() throws ClientException {
			DocumentModel doc = doGetOrCreateDoc(session, serverInstanceName);
			lastState = (String) doc
					.getPropertyValue(ADMINISTRATIVE_STATUS_PROPERTY);
			boolean isDirty = lastState == null
					|| !lastState.equals(newState);

			if (!isDirty) {
				return;
			}
			doc.setPropertyValue(ADMINISTRATIVE_STATUS_PROPERTY, newState);
			session.saveDocument(doc);
			session.save();
		}

	}

	protected static DocumentModel doGetOrCreateContainer(CoreSession session)
			throws ClientException {
		DocumentRef admRootDocRef = new PathRef("/"
				+ ADMINISTRATIVE_INFO_CONTAINER);
		if (!session.exists(admRootDocRef)) {
			DocumentModel doc = session.createDocumentModel("/",
					ADMINISTRATIVE_INFO_CONTAINER,
					"Folder");
			doc = session.createDocument(doc);
			session.save();
		}

		return session.getDocument(admRootDocRef);
	}

	protected DocumentModel doGetOrCreateDoc(CoreSession session, String serverInstanceName)
			throws ClientException {
		DocumentModel administrativeContainer = doGetOrCreateContainer(session);

		DocumentRef statusDocRef = new PathRef(
				administrativeContainer.getPathAsString() + "/"
						+ administrativeStatusDocName(serverInstanceName));

		if (!session.exists(statusDocRef)) {
			DocumentModel doc = session.createDocumentModel(
					administrativeContainer.getPathAsString(),
					administrativeStatusDocName(serverInstanceName),
					ADMINISTRATIVE_STATUS_DOCUMENT_TYPE);

			// set status active by default
			doc.setPropertyValue(ADMINISTRATIVE_STATUS_PROPERTY, AdministrativeStatus.ACTIVE);
			doc = session.createDocument(doc);
			session.save();

		}

		return session.getDocument(statusDocRef);

	}

	private class Fetcher extends UnrestrictedSessionRunner {

		protected final String serverInstanceName;
		
		private DocumentModel doc;
		
		private String administrativeStatusPropertyValue;

		public Fetcher(String repoName, String serverInstanceName) {
			super(repoName);
			this.serverInstanceName = serverInstanceName;
		}

		@Override
		public void run() throws ClientException {
			doc = doGetOrCreateDoc(session,serverInstanceName);
			administrativeStatusPropertyValue = (String)doc.getPropertyValue(
	                ADMINISTRATIVE_STATUS_PROPERTY);
		}

		public DocumentModel getDocument() {
			return doc;
		}
		
		public String getAdministrativeStatusPropertyValue(){
		    return administrativeStatusPropertyValue;
		}

	}

	protected String administrativeStatusDocName(String serverInstanceName) throws ClientException {
		return ADMINISTRATIVE_STATUS_DOCUMENT + "-" + serverInstanceName;
	}




	protected static EventProducer getEventProducer() throws Exception {
		return Framework.getService(EventProducer.class);
	}
	
    protected String getRepositoryName() {
        if (repositoryName == null) {
            repositoryName = Framework.getLocalService(RepositoryManager.class).getDefaultRepository().getName();
        }
        return repositoryName;
    }

}
