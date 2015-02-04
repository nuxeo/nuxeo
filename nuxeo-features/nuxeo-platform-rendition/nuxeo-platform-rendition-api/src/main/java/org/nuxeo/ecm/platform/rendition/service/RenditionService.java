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

package org.nuxeo.ecm.platform.rendition.service;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.RenditionException;

/**
 * Service handling Rendition Definitions and actual render based on a Rendition Definition
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.4.1
 */
public interface RenditionService {

    /**
     * Returns a {@code List} of registered {@code RenditionDefinition}. The order of the List does not depend on the
     * registering order.
     * @deprecated since 7.2. Not used.
     */
    @Deprecated
    List<RenditionDefinition> getDeclaredRenditionDefinitions();

    /**
     * Returns a {@code List} of registered {@code RenditionDefinition} matching a given provider type
     * @deprecated since 7.2. Not used
     */
    @Deprecated
    List<RenditionDefinition> getDeclaredRenditionDefinitionsForProviderType(String providerType);

    /**
     * Returns a {@code List} of {@code RenditionDefinition} available on the given Document. The order of the List does
     * not depend on the registering order.
     */
    List<RenditionDefinition> getAvailableRenditionDefinitions(DocumentModel doc);

    /**
     * Render a document based on the given rendition definition name and returns the stored Rendition
     * {@link DocumentRef}.
     * <p>
     * Only the user launching the render operation has the Read right on the returned document.
     *
     * @param sourceDocument the document to render
     * @param renditionDefinitionName the rendition definition to use
     * @return the {@code DocumentRef} of the newly created Rendition document.
     */
    DocumentRef storeRendition(DocumentModel sourceDocument, String renditionDefinitionName) throws RenditionException;

    /**
     * Return the {@link Rendition} object for the given {@link DocumentModel} and a rendition definition name.
     * <p>
     * A stored rendition is returned if found and up to date, a new Rendition is created otherwise.
     *
     * @param doc the document to render
     * @param renditionName the name of the rendition definition
     * @return the {@link Rendition} object
     * @throws RenditionException
     */
    Rendition getRendition(DocumentModel doc, String renditionName) throws RenditionException;

    /**
     * Return the {@link Rendition} object for the given {@link DocumentModel} and a rendition definition name.
     * <p>
     * A stored rendition is returned if found and up to date, a new (live) Rendition is created otherwise.
     * <p>
     * If store parameter is true, the new created rendition is stored too and returned
     *
     * @param doc the document to render
     * @param renditionName the name of the rendition definition
     * @param store indicates if the rendition must be stored
     * @return the {@link Rendition} object
     * @throws RenditionException
     */
    Rendition getRendition(DocumentModel doc, String renditionName, boolean store) throws RenditionException;

    /**
     * Returns a {@code List} of {@code Rendition} available on the given Document.
     * <p>
     * The order of the List does not depend on the registering order.
     * <p>
     * The returned rendition may be live or stored
     */
    List<Rendition> getAvailableRenditions(DocumentModel doc) throws RenditionException;

}
