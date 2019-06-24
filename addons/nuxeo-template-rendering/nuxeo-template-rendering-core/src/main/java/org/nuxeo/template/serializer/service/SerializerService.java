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


import org.nuxeo.template.serializer.executors.Serializer;

/**
 * Service Exposing serializer and deserializer used to manipulate template rendering data to be injected in
 * the rendition context. Here are the current service usage :
 * <ul>
 *     <li>API request => Inline context preparation : see in {@link org.nuxeo.template.automation.RenderWithTemplateOperation}</li>
 *     <li>Inline context preparation => store into the {@link org.nuxeo.template.api.adapters.TemplateBasedDocument}</li>
 *     <li>Context defined on Template creation => store into the {@link org.nuxeo.template.api.adapters.TemplateSourceDocument}</li>
 *     <li>And finally before rendition to collect data from TemplateSource and TemplateBased to generate the global context</li>
 * </ul>
 * You can create your own Serializer contributing to the extension point and call it on the API request. For instance,
 * if you want to send json instead XML.
 *
 * @Since 11.1
 */
public interface SerializerService {

    /**
     * Return the Serializer/Deserializer named id that transform List<TemplateInput> to a target serialized format.
     * Default serialized format is XML except if you override contributing a "default" serializer.
     * If no serializer named id, throws a NuxeoException.
     *
     * @param id : name of the requested serializer
     * @return the constructed serializer
     */
    Serializer getSerializer(String id);
}
