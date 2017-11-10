/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.platform.tag;

import static org.nuxeo.ecm.platform.tag.TagConstants.TAG_FACET;
import static org.nuxeo.ecm.platform.tag.TagConstants.TAG_LIST;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.versioning.VersioningPolicyFilter;

/**
 * @since 9.3
 */
public class NoVersioningFacetedTagFilter implements VersioningPolicyFilter {

    @Override
    public boolean test(DocumentModel previousDocument, DocumentModel currentDocument) {
        if (previousDocument == null || !currentDocument.hasFacet(TAG_FACET)) {
            return false;
        }
        for (String schema : currentDocument.getSchemas()) {
            for (Property prop : currentDocument.getPropertyObjects(schema)) {
                try {
                    String xpath = prop.getXPath();
                    if (!prop.isSameAs(previousDocument.getProperty(xpath)) && !TAG_LIST.equals(xpath)) {
                        return false;
                    }
                } catch (PropertyNotFoundException e) {
                    // Property could not exist in previous document because a facet was added to it dynamically
                    return false;
                }
            }
        }
        return true;
    }
}
