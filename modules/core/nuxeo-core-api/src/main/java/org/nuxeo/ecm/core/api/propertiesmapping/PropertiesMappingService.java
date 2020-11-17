/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     mcedica
 */
package org.nuxeo.ecm.core.api.propertiesmapping;

import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Service that allows to copy a set of metadata from a source to a target document
 *
 * @since 5.6
 */
public interface PropertiesMappingService {

    /**
     * Gets a map of xpaths defining properties on the target and source documents
     */
    Map<String, String> getMapping(String mappingName);

    /**
     * Copies the properties defined by the given xpaths in the mapping from the target to the source document. Assumes
     * that the xpaths are valid.
     */
    void mapProperties(CoreSession session, DocumentModel sourceDoc, DocumentModel targetDoc, String mappingName);

}
