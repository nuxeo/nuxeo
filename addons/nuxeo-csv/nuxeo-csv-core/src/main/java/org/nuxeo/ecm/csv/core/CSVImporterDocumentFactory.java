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
 *     Thomas Roger
 */

package org.nuxeo.ecm.csv.core;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;

import java.io.Serializable;
import java.util.Map;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public interface CSVImporterDocumentFactory extends Serializable {

    void createDocument(CoreSession session, String parentPath, String name, String type,
            Map<String, Serializable> values);

    void updateDocument(CoreSession session, DocumentRef docRef, Map<String, Serializable> values);

    /**
     * @return {@code true} if document with the specified parentPath, name, and values exists. {@code false} otherwise.
     * @since 8.2
     */
    boolean exists(CoreSession session, String parentPath, String name, Map<String, Serializable> values);

    /**
     * @deprecated since 8.2
     */
    @Deprecated boolean exists(CoreSession session, String parentPath, String name, String type,
            Map<String, Serializable> values);

    /**
     * @since 8.4
     */
    void setImporterOptions(CSVImporterOptions importerOptions);
}
