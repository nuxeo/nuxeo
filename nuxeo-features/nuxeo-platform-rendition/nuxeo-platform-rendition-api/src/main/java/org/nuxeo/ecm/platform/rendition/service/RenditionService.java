/*
 * (C) Copyright 2010-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *   Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.rendition.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.rendition.Rendition;

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
     *
     * @deprecated since 7.2 because unused
     */
    @Deprecated
    List<RenditionDefinition> getDeclaredRenditionDefinitions();

    /**
     * Returns a {@code List} of registered {@code RenditionDefinition} matching a given provider type
     *
     * @deprecated since 7.2 because unused
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
    DocumentRef storeRendition(DocumentModel sourceDocument, String renditionDefinitionName);

    /**
     * Return the {@link Rendition} object for the given {@link DocumentModel} and a rendition definition name.
     * <p>
     * A stored rendition is returned if found and up to date, a new Rendition is created otherwise.
     *
     * @param doc the document to render
     * @param renditionName the name of the rendition definition
     * @return the {@link Rendition} object
     */
    Rendition getRendition(DocumentModel doc, String renditionName);

    /**
     * Return the default {@link Rendition} object for the given {@link DocumentModel}.
     * <p>
     * A stored rendition is returned if found and up to date, a new Rendition is created otherwise.
     *
     * @param doc the document to render
     * @param reason the reason the rendition is being rendered (optional)
     * @param extendedInfos map of extended info added in the default rendition computation (optional)
     * @since 9.3
     * @return the default {@link Rendition} object
     */
    Rendition getDefaultRendition(DocumentModel doc, String reason, Map<String, Serializable> extendedInfos);

    /**
     * Return the {@link Rendition} object for the given {@link DocumentModel} and a rendition definition name.
     * <p>
     * A stored rendition is returned if found and up to date, a new (live) Rendition is created and returned otherwise.
     * <p>
     * If store parameter is true, the new created rendition is stored too unless it is marked as stale.
     *
     * @param doc the document to render
     * @param renditionName the name of the rendition definition
     * @param store indicates if the rendition must be stored
     * @return the {@link Rendition} object
     */
    Rendition getRendition(DocumentModel doc, String renditionName, boolean store);

    /**
     * Returns a {@code List} of {@code Rendition} available on the given Document.
     * <p>
     * The order of the List does not depend on the registering order.
     * <p>
     * The returned rendition may be live or stored
     */
    List<Rendition> getAvailableRenditions(DocumentModel doc);

    /**
     * Returns a {@code List} of {@code Rendition} available on the given Document.
     * <p>
     * If {@code onlyVisible} is true, returns only the rendition marked as visible.
     * <p>
     * The order of the List does not depend on the registering order.
     * <p>
     * The returned rendition may be live or stored
     *
     * @since 7.2
     */
    List<Rendition> getAvailableRenditions(DocumentModel doc, boolean onlyVisible);

    /**
     * Query and delete stored renditions where the related version or live document does not exist anymore.
     *
     * @since 8.4
     */
    void deleteStoredRenditions(String repositoryName);
}
