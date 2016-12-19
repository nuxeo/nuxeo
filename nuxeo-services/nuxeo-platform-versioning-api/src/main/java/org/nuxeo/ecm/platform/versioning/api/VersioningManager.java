/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Dragos Mihalache
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.versioning.api;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Interface for the Versioning Manager (a service).
 */
public interface VersioningManager {

    /**
     * Get document increment options as defined by versioning rules.
     *
     * @param doc the document
     * @return a list of version increment options available for the given document
     */
    VersionIncEditOptions getVersionIncEditOptions(DocumentModel doc);

    /**
     * Gets the label for the current version of a document, for the UI.
     *
     * @param doc the document
     * @return the version label
     */
    String getVersionLabel(DocumentModel doc);

}
