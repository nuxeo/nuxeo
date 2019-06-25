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

package org.nuxeo.template.serializer.executors;

import java.util.List;

import org.dom4j.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.template.api.TemplateInput;

/**
 * (was org.nuxeo.template.XMLSerializer).
 * {@link TemplateInput} parameters are stored in the {@link DocumentModel} as a single String Property via XML
 * Serialization. This class contains the Serialization/Deserialization logic.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @author bjalon (bjalon@qastia.com)
 *
 * @Since 11.1
 */
public interface Serializer {

    /**
     * Transform String to a List of TemplateInput. TemplateInput represent a field that will be finally added into the
     * context of the file rendition. Please for more information look the documentation of
     * {@link org.nuxeo.template.serializer.service.SerializerService}
     *
     * @param content : String containing a list of fields' description serialized
     * @return the content deserialized
     * @throws DocumentException
     */
    List<TemplateInput> doDeserialization(String content) throws DocumentException;

    /**
     * Transform the List of TemplateInput to String. Used to store a rendition context in the Document TemplateBased.
     * Please for more information look the documentation of
     * {@link org.nuxeo.template.serializer.service.SerializerService}
     *
     * @param content
     * @return
     */
    String doSerialization(List<TemplateInput> content);
}
