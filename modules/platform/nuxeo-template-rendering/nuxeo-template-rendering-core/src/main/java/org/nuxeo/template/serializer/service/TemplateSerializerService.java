/*
 * (C) Copyright 2019 Qastia (http://www.qastia.com/) and others.
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
 *     Benjamin JALON
 *
 */

package org.nuxeo.template.serializer.service;

import java.util.List;

import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.serializer.executors.TemplateSerializer;

/**
 * Service Exposing serializer and deserializer used to manipulate template rendering data to be injected in the
 * rendition context. Here are the current service usage :
 * <ul>
 * <li>API request =&gt; Inline context preparation : see in
 * {@link org.nuxeo.template.automation.RenderWithTemplateOperation}</li>
 * <li>Inline context preparation =&gt; store into the
 * {@link org.nuxeo.template.api.adapters.TemplateBasedDocument}</li>
 * <li>Context defined on Template creation =&gt; store into the
 * {@link org.nuxeo.template.api.adapters.TemplateSourceDocument}</li>
 * <li>And finally before rendition to collect data from TemplateSource and TemplateBased to generate the global
 * context</li>
 * </ul>
 * You can create your own Serializer contributing to the extension point and call it on the API request. For instance,
 * if you want to send json instead XML.
 *
 * @since 11.1
 */
public interface TemplateSerializerService {

    /**
     * Returns the {@link TemplateSerializer} with the given {@code id}.
     * <p>
     * If no {@link TemplateSerializer} is found, return the 'default' one.
     *
     * @throws org.nuxeo.ecm.core.api.NuxeoException if no {@code id} or 'default' {@link TemplateSerializer} found
     */
    TemplateSerializer getSerializer(String id);

    /**
     * Convenient method to serialize {@code params} to XML using the 'xml' {@link TemplateSerializer}.
     */
    default String serializeXML(List<TemplateInput> params) {
        return getSerializer("xml").serialize(params);
    }

    /**
     * Convenient method to deserialize XML content using the 'xml' {@link TemplateSerializer}..
     */
    default List<TemplateInput> deserializeXML(String content) {
        return getSerializer("xml").deserialize(content);
    }

}
