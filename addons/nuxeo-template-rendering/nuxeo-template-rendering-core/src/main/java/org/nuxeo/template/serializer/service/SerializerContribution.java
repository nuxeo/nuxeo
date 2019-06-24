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

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.Descriptor;
import org.nuxeo.template.serializer.executors.Serializer;

/**
 * @Since 11.1
 */
@XObject("serializer")
public class SerializerContribution implements Descriptor {

    @XNode("@class")
    public Class<Serializer> implementationClass;

    @XNode("@name")
    public String name;

    @Override
    public String getId() {
        return StringUtils.defaultIfBlank(name, "default");
    }

    public Serializer getImplementation() {
        Serializer obj;
        try {
            obj = implementationClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("Serializer Contribution Exception on Serializer construction: " + name, e);
        }
        return obj;
    }
}
