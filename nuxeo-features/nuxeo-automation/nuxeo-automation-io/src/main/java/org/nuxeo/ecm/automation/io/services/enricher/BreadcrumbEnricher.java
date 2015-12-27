/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.io.services.enricher;

import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonDocumentListWriter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.BreadcrumbJsonEnricher;

/**
 * @since 5.7.3
 * @deprecated This enricher was migrated to {@link BreadcrumbJsonEnricher}. The content enricher service doesn't work
 *             anymore.
 */
@Deprecated
public class BreadcrumbEnricher extends AbstractContentEnricher {

    @Override
    public void enrich(JsonGenerator jg, RestEvaluationContext ec) throws IOException {
        DocumentModel doc = ec.getDocumentModel();
        CoreSession session = doc.getCoreSession();
        List<DocumentModel> parentDocuments = session.getParentDocuments(doc.getRef());
        JsonDocumentListWriter.writeDocuments(jg, new DocumentModelListImpl(parentDocuments), new String[] {},
                ec.getRequest());
    }

}
