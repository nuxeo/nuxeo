/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.htmlsanitizer;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Service that sanitizes some HTML fields to remove potential cross-site scripting attacks in them.
 */
public interface HtmlSanitizerService {

    /**
     * Sanitizes a document's fields, depending on the service configuration.
     */
    void sanitizeDocument(DocumentModel doc);

    /**
     * Sanitizes a string.
     *
     * @param html the string to sanitize
     * @param info additional info logged when something is sanitized
     * @return the sanitized string
     */
    String sanitizeString(String html, String info);

}
