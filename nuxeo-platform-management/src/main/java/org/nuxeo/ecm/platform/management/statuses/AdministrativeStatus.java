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

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Used to control the server administrative status: the status of the server
 * can be locked/unlocked.
 * 
 * @author Mariana Cedica
 */
public class AdministrativeStatus implements AdministrativeStatusConstants {

	private static final Log log = LogFactory
			.getLog(AdministrativeStatus.class);

	protected EventProducer eventProducer;

	/**
	 * Disable services for this server
	 * 
	 * @return true if the locked succeed
	 */
	public boolean lockServer() {
		AdministrativeStatusServerChangeState runner = new AdministrativeStatusServerChangeState(
				repositoryName, LOCKED, LOCKED_EVENT);
		try {
			runner.runUnrestricted();
			return true;
		} catch (ClientException e) {
			log.error("Unable to lock server", e);
			return false;
		}
	}

	/**
	 * Enable services for this server
	 * 
	 * @param session
	 * @return
	 */
	public boolean unlockServer() {
		AdministrativeStatusServerChangeState runner = new AdministrativeStatusServerChangeState(
				repositoryName, UNLOCKED, UNLOCKED_EVENT);
		try {
			runner.runUnrestricted();
			return true;
		} catch (ClientException e) {
			log.error("Unable to lock server", e);
			return false;
		}
	}

	/**
	 * Is locked/unlocked
	 * 
	 * @param session
	 * @return
	 * @throws ClientException
	 */
	public String getServerStatus() throws ClientException {
		return (String) getOrCreateStatusDocument().getPropertyValue(
				ADMINISTRATIVE_STATUS_PROPERTY);
	}

	public boolean isUnlocked() throws ClientException {
		return getServerStatus().equals(UNLOCKED);
	}

	public boolean isLocked() throws ClientException {
		return getServerStatus().equals(LOCKED);
	}

	private DocumentModel getOrCreateStatusDocument() throws ClientException {
		AdministrativeStatusFetcher fetcher = new AdministrativeStatusFetcher(
				repositoryName);
		try {
			fetcher.runUnrestricted();
		} catch (ClientException e) {
			log.error("Unable to fetch the administrative status document", e);
			throw new ClientException(e);
		}
		return fetcher.getDocument();
	}

	protected String repositoryName = Framework
			.getLocalService(RepositoryManager.class).getDefaultRepository()
			.getName();

	private class AdministrativeStatusServerChangeState extends
			UnrestrictedSessionRunner {

		private String serverState;

		private String eventName;

		public AdministrativeStatusServerChangeState(String repoName,
				String state, String eventName) {
			super(repoName);
			this.serverState = state;
			this.eventName = eventName;

		}

		@Override
		public void run() throws ClientException {
			DocumentModel doc = doGetOrCreateDoc(session);
			String actualServerState = (String) doc
					.getPropertyValue(ADMINISTRATIVE_STATUS_PROPERTY);
			if (actualServerState != null
					&& actualServerState.equals(serverState)) {
				return;
			}

			doc.setPropertyValue(ADMINISTRATIVE_STATUS_PROPERTY, serverState);
			session.saveDocument(doc);
			session.save();
			// and notify docStatus created
			Map<String, Serializable> eventProperties = new HashMap<String, Serializable>();
			eventProperties.put("category", ADMINISTRATIVE_EVENT_CATEGORY);
			notifyEvent(session, eventName, doc, eventProperties);
		}

	}

	protected static DocumentModel doGetOrCreateContainer(CoreSession session)
			throws ClientException {
		DocumentRef admRootDocRef = new PathRef("/"
				+ ADMINISTRATIVE_INFO_CONTAINER);
		if (!session.exists(admRootDocRef)) {
			DocumentModel model = session.createDocumentModel("/",
					ADMINISTRATIVE_INFO_CONTAINER,
					ADMINISTRATIVE_INFO_CONTAINER_TYPE);
			model = session.createDocument(model);
			session.save();
		}

		return session.getDocument(admRootDocRef);
	}

	protected static DocumentModel doGetOrCreateDoc(CoreSession session)
			throws ClientException {
		DocumentModel administrativeContainer = doGetOrCreateContainer(session);

		DocumentRef statusDocRef = new PathRef(
				administrativeContainer.getPathAsString() + "/"
						+ administrativeStatusDocName());

		if (!session.exists(statusDocRef)) {
			DocumentModel doc = session.createDocumentModel(
					administrativeContainer.getPathAsString(),
					administrativeStatusDocName(),
					ADMINISTRATIVE_STATUS_DOCUMENT_TYPE);

			// set status unlocked by default
			doc.setPropertyValue(ADMINISTRATIVE_STATUS_PROPERTY, UNLOCKED);
			doc = session.createDocument(doc);
			session.save();

			// and notify docStatus created
			Map<String, Serializable> eventProperties = new HashMap<String, Serializable>();
			eventProperties.put("category", ADMINISTRATIVE_EVENT_CATEGORY);
			notifyEvent(session, ADMINISTRATIVE_STATUS_DOC_CREATED_EVENT,
					doc, eventProperties);

		}

		return session.getDocument(statusDocRef);

	}

	private class AdministrativeStatusFetcher extends UnrestrictedSessionRunner {

		private DocumentModel doc;

		public AdministrativeStatusFetcher(String repoName) {
			super(repoName);
		}

		@Override
		public void run() throws ClientException {
			doc = doGetOrCreateDoc(session);
		}

		public DocumentModel getDocument() {
			return doc;
		}

	}

	protected static String administrativeStatusDocName() throws ClientException {
		return ADMINISTRATIVE_STATUS_DOCUMENT + "-" + getServerInstanceName();
	}

	public static String getServerInstanceName() throws ClientException {
		String instanceName = Framework.getProperties().getProperty(
				ADMINISTRATIVE_INSTANCE_ID);
		if (StringUtils.isEmpty(instanceName)) {
			InetAddress addr;
			try {
				addr = InetAddress.getLocalHost();
				instanceName = addr.getHostName();
			} catch (UnknownHostException e) {
				instanceName = "localhost";
			}
		}
		return instanceName;
	}

	protected static void notifyEvent(CoreSession session, String name,
			DocumentModel document, Map<String, Serializable> eventProperties) {
		DocumentEventContext envContext = new DocumentEventContext(session,
				session.getPrincipal(), document);
		envContext.setProperties(eventProperties);
		try {
			getEventProducer().fireEvent(envContext.newEvent(name));
		} catch (Exception e) {
			log.error("Unable to fire event", e);
		}
	}

	protected static EventProducer getEventProducer() throws Exception {
		return Framework.getService(EventProducer.class);
	}

}
