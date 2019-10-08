/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     - Ku Chang <kchang@nuxeo.com>
 */
package org.nuxeo.audit.storage.stream;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;

public class AuditRepositoryInit extends DefaultRepositoryInit {

	public static final String YOUPS_PATH = "/youps";

	@Override
	public void populate(CoreSession session) {
		super.populate(session);
		DocumentModel rootDocument = session.getRootDocument();
		DocumentModel model = session.createDocumentModel(rootDocument.getPathAsString(), "youps", "File");
		model.setProperty("dublincore", "title", "huum");
		session.createDocument(model);
	}
}