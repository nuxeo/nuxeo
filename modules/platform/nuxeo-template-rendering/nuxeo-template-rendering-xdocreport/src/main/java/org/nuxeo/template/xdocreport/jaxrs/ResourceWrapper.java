/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.template.xdocreport.jaxrs;

import org.nuxeo.template.api.adapters.TemplateSourceDocument;

import fr.opensagres.xdocreport.remoting.resources.domain.Resource;
import fr.opensagres.xdocreport.remoting.resources.domain.ResourceType;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class ResourceWrapper {

    public static Resource wrap(TemplateSourceDocument srcDocument) {
        Resource rs = new Resource();
        rs.setType(ResourceType.TEMPLATE);

        rs.setName(srcDocument.getName());
        rs.setId(srcDocument.getId());

        Resource fileResource = new NonRecursiveResource();
        fileResource.setName(srcDocument.getFileName());
        fileResource.setId(srcDocument.getId());
        fileResource.setType(ResourceType.DOCUMENT);

        Resource METAResource = new NonRecursiveResource();
        METAResource.setName("META-INF");
        METAResource.setId(srcDocument.getId() + "/META-INF");
        METAResource.setType(ResourceType.CATEGORY);

        Resource fieldResource = new NonRecursiveResource();
        fieldResource.setName(srcDocument.getName() + ".fields.xml");
        fieldResource.setId(srcDocument.getId() + ".fields.xml");
        fieldResource.setType(ResourceType.DOCUMENT);

        METAResource.getChildren().add(fieldResource);

        rs.getChildren().add(fileResource);
        rs.getChildren().add(METAResource);

        return rs;
    }
}
