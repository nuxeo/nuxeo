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

import javax.servlet.ServletRequest;
import javax.ws.rs.core.HttpHeaders;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Evaluation context that knows about the current document and the HTTP request headers.
 *
 * @since 5.7.3
 * @deprecated The JSON marshalling was migrated to nuxeo-core-io. An enricher system is also available. See
 *             org.nuxeo.ecm.core.io.marshallers.json.enrichers.BreadcrumbJsonEnricher for an example. To migrate an
 *             existing enricher, keep the marshalling code and use it in class implementing
 *             AbstractJsonEnricher&lt;DocumentModel&gt; (the use of contextual parameters is a bit different but
 *             compatible / you have to manage the enricher's parameters yourself). Don't forget to contribute to
 *             service org.nuxeo.ecm.core.io.registry.MarshallerRegistry to register your enricher.
 */
@Deprecated
public class HeaderDocEvaluationContext implements RestEvaluationContext {

    private DocumentModel doc;

    private HttpHeaders headers;

    private ServletRequest request;

    /**
     * Creates the evaluation context.
     *
     * @param doc
     * @param headers
     */
    public HeaderDocEvaluationContext(DocumentModel doc, HttpHeaders headers, ServletRequest request) {
        this.doc = doc;
        this.headers = headers;
        this.request = request;
    }

    @Override
    public DocumentModel getDocumentModel() {
        return doc;
    }

    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }

    @Override
    public ServletRequest getRequest() {
        return request;
    }

}
