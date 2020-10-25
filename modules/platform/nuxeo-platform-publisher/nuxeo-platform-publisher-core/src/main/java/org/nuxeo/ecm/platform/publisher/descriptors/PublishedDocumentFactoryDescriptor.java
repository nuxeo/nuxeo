/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.descriptors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;

/**
 * Descriptor used to register PublishedDocument factories.
 *
 * @author tiry
 */
@XObject("publishedDocumentFactory")
public class PublishedDocumentFactoryDescriptor {

    @XNode("@name")
    private String name;

    @XNode("@class")
    private Class<? extends PublishedDocumentFactory> klass;

    @XNode("@validatorsRule")
    private String validatorsRuleName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<? extends PublishedDocumentFactory> getKlass() {
        return klass;
    }

    public void setKlass(Class<? extends PublishedDocumentFactory> klass) {
        this.klass = klass;
    }

    public String getValidatorsRuleName() {
        return validatorsRuleName;
    }

}
