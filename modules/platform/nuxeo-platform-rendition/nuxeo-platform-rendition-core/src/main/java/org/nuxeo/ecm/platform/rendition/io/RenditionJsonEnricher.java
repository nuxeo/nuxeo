/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Gabriel Barata
 */

package org.nuxeo.ecm.platform.rendition.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 8.1
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class RenditionJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String RENDITION_REST_URL_FORMAT = "%sapi/v1/id/%s/@rendition/%s";

    public static final String NAME = "renditions";

    public RenditionJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        RenditionService renditionService = Framework.getService(RenditionService.class);
        List<Rendition> renditions = renditionService.getAvailableRenditions(document, true);
        jg.writeArrayFieldStart(NAME);
        for (Rendition rendition : renditions) {
            jg.writeStartObject();
            jg.writeStringField("name", rendition.getName());
            jg.writeStringField("kind", rendition.getKind());
            jg.writeStringField("icon", ctx.getBaseUrl().replaceAll("/$", "") + rendition.getIcon());
            jg.writeStringField("url", String.format(RENDITION_REST_URL_FORMAT, ctx.getBaseUrl(), document.getId(),
                rendition.getName()));
            jg.writeEndObject();
        }
        jg.writeEndArray();
    }

}
