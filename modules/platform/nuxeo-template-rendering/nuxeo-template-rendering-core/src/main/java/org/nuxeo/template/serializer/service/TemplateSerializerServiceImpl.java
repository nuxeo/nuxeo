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

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.template.serializer.executors.TemplateSerializer;

/**
 * @since 11.1
 */
public class TemplateSerializerServiceImpl extends DefaultComponent implements TemplateSerializerService {

    /**
     * Extention point name
     */
    protected static final String EXTENSION_POINT_NAME = "serializers";

    /**
     * The well-known name of the default serializer
     */
    protected static final String DEFAULT_SERIALIZER_NAME = "default";

    @Override
    public TemplateSerializer getSerializer(String id) {
        String finalId = StringUtils.defaultIfBlank(id, DEFAULT_SERIALIZER_NAME);
        return this.<SerializerDescriptor> getRegistryContribution(EXTENSION_POINT_NAME, finalId)
                   .or(() -> getRegistryContribution(EXTENSION_POINT_NAME, DEFAULT_SERIALIZER_NAME))
                   .map(SerializerDescriptor::newInstance)
                   .orElseThrow(() -> new NuxeoException("UnknownSerializer named " + id));
    }

}
