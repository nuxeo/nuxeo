/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 * Interface hiding the actual rendition implementation and allowing for Lazy
 * computation of the rendition blobs.
 * <p>
 * RenditionDefinition is partially wrapper in the {@link Rendition}
 * 
 * @since 5.6
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public interface Rendition {

    /**
     * get icon file name
     * 
     * @return
     */
    String getIcon();

    /**
     * get the {@link RenditionDefinition} name
     * 
     * @return
     */
    String getName();

    /**
     * get the {@link RenditionDefinition} label
     * 
     * s@return
     */
    String getLabel();

    /**
     * Get the King of the {@link RenditionDefinition}
     * 
     * @return
     */
    String getKind();

    /**
     * return the type of the provider that was used to generate the rendition
     * 
     * @return
     */
    String getProviderType();

    /**
     * Indicates if the Rendition is stored or live
     * 
     * @return
     */
    boolean isStored();

    /**
     * Return rendered Blob
     * 
     * @return
     * @throws RenditionException
     */
    Blob getBlob() throws RenditionException;

    /**
     * Return rendered Blobs
     * 
     * @return
     * @throws RenditionException
     */
    List<Blob> getBlobs() throws RenditionException;

    /**
     * Return the Document hosting the rendition.
     * <p>
     * In case of a Live rendition it will be the target document and in case of
     * stored Rendition it will be the Rendition document it self
     * 
     * @return
     */
    DocumentModel getHostDocument();

    /**
     * Get last modification date.
     * <p>
     * returns current time for live renditions
     * 
     * @return
     */

    Calendar getModificationDate();

}
