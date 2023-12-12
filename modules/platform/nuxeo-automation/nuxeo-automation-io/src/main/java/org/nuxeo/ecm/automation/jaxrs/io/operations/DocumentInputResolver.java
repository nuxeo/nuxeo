/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.automation.jaxrs.io.operations;

import org.nuxeo.ecm.automation.core.impl.adapters.helper.AbsoluteDocumentRef;
import org.nuxeo.ecm.automation.core.operations.blob.BulkDownload;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.runtime.api.Framework;

/**
 * @author matic
 */
public class DocumentInputResolver implements InputResolver<DocumentRef> {

    /**
     * Framework property to allow calling the {@link BulkDownload} operation with a list of documents from multiple
     * repositories as an input.
     *
     * @since 2023.5
     */
    public static final String BULK_DOWNLOAD_MULTI_REPOSITORIES = "nuxeo.bulk.download.multi.repositories";

    @Override
    public String getType() {
        return "doc";
    }

    @Override
    public DocumentRef getInput(String content) {
        return docRefFromString(content);
    }

    public static DocumentRef docRefFromString(String input) {
        if (Framework.isBooleanPropertyTrue(BULK_DOWNLOAD_MULTI_REPOSITORIES)) {
            if (input.startsWith("/")) {
                // relative path ref, e.g. "/some/path" (could include some ":", e.g. /some/pa:th)
                return new PathRef(input);
            }

            int index = input.indexOf(":");
            if (index == -1) {
                // relative id ref, e.g. "someid" (cannot include any ":", otherwise it's an absolute ref)
                return new IdRef(input);
            }

            // absolute path or id ref, e.g. "repositoryName:docPath|docId"
            String repositoryName = input.substring(0, index);
            String docRef = input.substring(index + 1);
            return new AbsoluteDocumentRef(repositoryName, relativeDocRefFromString(docRef));
        }
        // relative path or id ref, e.g. "/some/path" or "someid"
        return relativeDocRefFromString(input);
    }

    /** @since 2023.5 */
    protected static DocumentRef relativeDocRefFromString(String input) {
        if (input.startsWith("/")) {
            return new PathRef(input);
        } else {
            return new IdRef(input);
        }
    }

}
