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

package org.nuxeo.ecm.platform.rendition;

import java.util.Calendar;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;

/**
 * Interface hiding the actual rendition implementation and allowing for Lazy computation of the rendition blobs.
 * <p>
 * RenditionDefinition is partially wrapper in the {@link Rendition}
 *
 * @since 5.6
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public interface Rendition {

    /**
     * Returns icon file name
     */
    String getIcon();

    /**
     * Returns the {@link RenditionDefinition} name
     */
    String getName();

    /**
     * Returns the {@link RenditionDefinition} CMIS name
     *
     * @since 7.3
     */
    String getCmisName();

    /**
     * Returns the {@link RenditionDefinition} label
     */
    String getLabel();

    /**
     * Returns the King of the {@link RenditionDefinition}
     */
    String getKind();

    /**
     * Returns the type of the provider that was used to generate the rendition
     */
    String getProviderType();

    /**
     * Indicates if the Rendition is stored or live
     */
    boolean isStored();

    /**
     * Returns the rendered Blob or {@code null} if none.
     */
    Blob getBlob();

    /**
     * Returns the rendered Blobs or {@code null} if none.
     */
    List<Blob> getBlobs();

    /**
     * Return the Document hosting the rendition.
     * <p>
     * In case of a Live rendition it will be the target document and in case of stored Rendition it will be the
     * Rendition document it self
     */
    DocumentModel getHostDocument();

    /**
     * Returns last modification date.
     * <p>
     * Returns current time for live renditions.
     */
    Calendar getModificationDate();

    /**
     * Checks if this rendition's computation has completed.
     *
     * @since 7.10
     */
    boolean isCompleted();

}
