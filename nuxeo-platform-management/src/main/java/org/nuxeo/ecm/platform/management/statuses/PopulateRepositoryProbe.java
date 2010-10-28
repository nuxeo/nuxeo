/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.platform.management.statuses;

import java.util.Calendar;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.management.api.Probe;
import org.nuxeo.ecm.core.management.api.ProbeStatus;
import org.nuxeo.ecm.core.management.storage.DocumentStoreSessionRunner;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class PopulateRepositoryProbe implements Probe {

        public static class Runner extends DocumentStoreSessionRunner {

        protected String info;

        @Override
        public void run() throws ClientException {
            DocumentModel rootDocument = session.getRootDocument();
            String name = String.format("%s:%x", PopulateRepositoryProbe.class.getSimpleName(), Calendar.getInstance().getTimeInMillis());
            DocumentModel doc = session.createDocumentModel(rootDocument.getPathAsString(), name, "File");
            doc.setProperty("dublincore", "title", name);
            doc.setProperty("uid", "major_version", 1L);
            doc = session.createDocument(doc);
            session.removeDocument(doc.getRef());
            info = "Created document " + doc.getPathAsString() + " and  removed it ";
        }

    }

    @Override
    public ProbeStatus run() {
        Runner runner = new Runner();
        try {
            runner.runUnrestricted();
        } catch (ClientException e) {
            return ProbeStatus.newError(e);
        }
        return  ProbeStatus.newSuccess(runner.info);
    }

}
