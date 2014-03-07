/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.automation.io.services.contributor;

import javax.servlet.ServletRequest;
import javax.ws.rs.core.HttpHeaders;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 *
 * Evaluation context that knows about the current document and the HTTP request
 * headers.
 *
 * @since 5.7.3
 */
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
