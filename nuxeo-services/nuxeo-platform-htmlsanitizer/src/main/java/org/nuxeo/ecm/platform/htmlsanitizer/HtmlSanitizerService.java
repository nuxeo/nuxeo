/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.htmlsanitizer;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Service that sanitizes some HTML fields to remove potential cross-site
 * scripting attacks in them.
 */
public interface HtmlSanitizerService {

    /**
     * Sanitizes a document's fields, depending on the service configuration.
     */
    void sanitizeDocument(DocumentModel doc) throws ClientException;

    /**
     * Sanitizes a string.
     *
     * @param html the string to sanitize
     * @param info additional info logged when something is sanitized
     * @return the sanitized string
     */
    String sanitizeString(String html, String info);

}
