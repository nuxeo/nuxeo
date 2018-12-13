/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.platform.rendition.publisher;

import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinitionProvider;

/**
 * Simple {@link RenditionDefinitionProvider} to test rendition publication.
 *
 * @since 10.10
 */
public class PublicationRenditionDefinitionProvider implements RenditionDefinitionProvider {

    @Override
    public List<RenditionDefinition> getRenditionDefinitions(DocumentModel doc) {
        RenditionDefinition renditionDefinition = new RenditionDefinition();
        renditionDefinition.setName("publicationRendition");
        renditionDefinition.setProvider(new PublicationRenditionProvider());
        return Collections.singletonList(renditionDefinition);
    }

}
