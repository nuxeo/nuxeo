/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.pathelements;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.FacetNames;

/**
 * Represents a document path element, with no visible link.
 * <p>
 * Useful when representing documents in the breadcrumbs that are marked as {@link FacetNames#HIDDEN_IN_NAVIGATION}.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class HiddenDocumentPathElement extends DocumentPathElement {

    private static final long serialVersionUID = 1L;

    public HiddenDocumentPathElement(DocumentModel docModel) {
        super(docModel);
    }

    @Override
    public boolean isLink() {
        return false;
    }

}
