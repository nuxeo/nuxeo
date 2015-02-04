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

package org.nuxeo.ecm.platform.rendition.extension;

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.RenditionException;
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
    List<Blob> render(DocumentModel doc, RenditionDefinition definition) throws RenditionException;

}
