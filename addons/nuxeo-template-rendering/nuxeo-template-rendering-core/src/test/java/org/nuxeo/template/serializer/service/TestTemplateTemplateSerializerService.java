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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.template.serializer.executors.TemplateSerializer;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@Deploy("org.nuxeo.template.manager:OSGI-INF/serializer-service.xml")
@Deploy("org.nuxeo.template.manager:OSGI-INF/serializer-service-contribution.xml")
public class TestTemplateTemplateSerializerService {

    @Inject
    protected TemplateSerializerService templateSerializerService;

    @Test
    public void serviceShouldThere() {
        assertNotNull(templateSerializerService);
    }

    @Test
    public void defaultSerializerShouldBeTheXMLOne() {
        TemplateSerializer templateSerializer = templateSerializerService.getSerializer(null);
        assertNotNull(templateSerializer);
        assertEquals("XMLTemplateSerializer", templateSerializer.getClass().getSimpleName());
    }

    @Test
    public void whenRequestXMLSerializer_shouldReturnXMLSerializer() {
        TemplateSerializer templateSerializer = templateSerializerService.getSerializer("xml");
        assertNotNull(templateSerializer);
        assertEquals("XMLTemplateSerializer", templateSerializer.getClass().getSimpleName());
    }

}
