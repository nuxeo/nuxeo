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
package org.nuxeo.ecm.automation.io.services.enricher;

import javax.servlet.ServletRequest;
import javax.ws.rs.core.HttpHeaders;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Evaluation context from where to get information in order to generate
 * additional JSON.
 *
 * @since 5.7.3
 */
public interface RestEvaluationContext {

    /**
     * Gives the contextual document
     *
     * @return
     *
     */
    DocumentModel getDocumentModel();

    /**
     * Returns the request headers. It may be used by contributors to refine
     * their writing strategies.
     *
     * @return
     *
     */
    HttpHeaders getHeaders();

    /**
     * Returns the request that is currently served.
     *
     * @since 5.9.3
     */
    ServletRequest getRequest();

}
