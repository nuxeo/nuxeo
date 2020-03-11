/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.ecm.platform.management.statuses;

import java.util.Calendar;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.management.storage.DocumentStoreSessionRunner;
import org.nuxeo.runtime.management.api.Probe;
import org.nuxeo.runtime.management.api.ProbeStatus;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class PopulateRepositoryProbe implements Probe {

    public static class Runner extends DocumentStoreSessionRunner {

        protected String info;

        @Override
        public void run() {
            DocumentModel rootDocument = session.getRootDocument();
            String name = String.format("%s:%x", PopulateRepositoryProbe.class.getSimpleName(),
                    Calendar.getInstance().getTimeInMillis());
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
        runner.runUnrestricted();
        return ProbeStatus.newSuccess(runner.info);
    }

}
