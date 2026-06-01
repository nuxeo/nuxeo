/*
 * (C) Copyright 2006-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 *
 */
package org.nuxeo.ecm.core.api.thumbnail;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 5.7
 */
public interface ThumbnailService {

    /**
     * Get the document thumbnail (related to the doc type/facet)
     */
    Blob getThumbnail(DocumentModel doc, CoreSession session);

    /**
     * Compute the thumbnail (related to the document type/facet)
     */
    Blob computeThumbnail(DocumentModel doc, CoreSession session);

    /**
     * Returns the thumbnail service configuration for the given repository.
     * <p>
     * The descriptor with id {@link ThumbnailConfigDescriptor#DEFAULT_ID} is overlaid by the descriptor matching
     * {@code repositoryName} (if any). Never returns {@code null}: when no contribution is present, a descriptor with
     * default values is returned.
     *
     * @since 2025.22
     */
    ThumbnailConfigDescriptor getConfiguration(String repositoryName);

}
