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
        SerializerDescriptor contrib = getDescriptor(EXTENSION_POINT_NAME, id);

        if (contrib == null) {
            contrib = getDescriptor(EXTENSION_POINT_NAME, DEFAULT_SERIALIZER_NAME);
            if (contrib == null) {
                throw new NuxeoException("UnknownSerializer named " + id);
            }
        }

        return contrib.newInstance();
    }

}
