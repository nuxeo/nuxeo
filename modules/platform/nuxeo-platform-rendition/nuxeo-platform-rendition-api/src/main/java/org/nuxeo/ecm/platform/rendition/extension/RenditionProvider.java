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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition.extension;

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;

/**
 * Interface to hide providers of {@link Rendition}. A provider could be converter based, template based, or Automation
 * based
 *
 * @since 5.6
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public interface RenditionProvider {

    /**
     * Test if the Rendition is available on the given DocumentModel
     */
    boolean isAvailable(DocumentModel doc, RenditionDefinition definition);

    /**
     * Generate the rendition Blobs for a given {@link RenditionDefinition}. Return is a List of Blob for bigger
     * flexibility (typically HTML rendition with resources)
     *
     * @param doc the target {@link DocumentModel}
     * @param definition the {@link RenditionDefinition} to use
     * @return The list of Blobs
     */
    List<Blob> render(DocumentModel doc, RenditionDefinition definition);

    /**
     * Gets the optional {@link org.nuxeo.ecm.platform.rendition.Constants#RENDITION_VARIANT_PROPERTY
     * RENDITION_VARIANT_PROPERTY} value for a given {@link RenditionDefinition}.
     *
     * @param doc the target document
     * @param definition the rendition definition to use
     * @return the generated {@link org.nuxeo.ecm.platform.rendition.Constants#RENDITION_VARIANT_PROPERTY
     *         RENDITION_VARIANT_PROPERTY} value, or {@code null}
     * @since 8.1
     */
    default String getVariant(DocumentModel doc, RenditionDefinition definition) {
        return null;
    }

}
