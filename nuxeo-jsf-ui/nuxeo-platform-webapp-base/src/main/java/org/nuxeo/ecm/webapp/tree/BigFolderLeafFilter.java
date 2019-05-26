/*
 * (C) Copyright 2008 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.webapp.tree;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.schema.FacetNames;

/**
 * Simple filter that accepts if the document has the {@code BigFolder} facet.
 *
 * @author Florent Guillaume
 */
public class BigFolderLeafFilter implements Filter {

    private static final long serialVersionUID = 1L;

    /**
     * @deprecated since 8.4, use {@link (org.nuxeo.ecm.core.schema.FacetNames.BIG_FOLDER)} instead.
     */
    @Deprecated
    public static final String BIG_FOLDER_FACET = "BigFolder";

    /**
     * Accepts if the document has the {@code BigFolder} facet.
     */
    @Override
    public boolean accept(DocumentModel document) {
        return document.hasFacet(FacetNames.BIG_FOLDER);
    }

}
