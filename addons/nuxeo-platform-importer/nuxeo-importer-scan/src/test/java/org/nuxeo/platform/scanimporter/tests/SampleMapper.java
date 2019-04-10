/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.platform.scanimporter.tests;

import java.io.File;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.tree.DefaultElement;
import org.nuxeo.ecm.platform.scanimporter.processor.DocumentTypeMapper;

/**
 * Sample {@link DocumentTypeMapper} impl : get document type from XML
 *
 * @author Thierry Delprat
 */
public class SampleMapper implements DocumentTypeMapper {

    @Override
    public String getTargetDocumentType(Document xmlDoc, File file) {
        List<?> nodes = xmlDoc.selectNodes("//resource-definition");
        if (nodes.size() >= 1) {
            DefaultElement elem = (DefaultElement) nodes.get(0);
            return elem.attribute("name").getValue();
        } else {
            return "File";
        }
    }
}
