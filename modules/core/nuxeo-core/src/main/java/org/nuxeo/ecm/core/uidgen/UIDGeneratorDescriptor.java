/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Dragos Mihalache
 */
package org.nuxeo.ecm.core.uidgen;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * UID generator configuration holder.
 */
@XObject("generator")
@XRegistry(compatWarnOnMerge = true)
public class UIDGeneratorDescriptor {

    @XNode("@name")
    @XRegistryId
    private String name;

    @XNode("@class")
    private Class<? extends UIDGenerator> generatorClass;

    /** @since 11.5 **/
    @XNode(value = "counterStart", defaultAssignment = "1")
    private int counterStart;

    @XNodeList(value = "propertyName", type = String[].class, componentType = String.class)
    private String[] propertyNames;

    @XNodeList(value = "docType", type = String[].class, componentType = String.class)
    private String[] docTypes;

    /** @since 11.5 **/
    public UIDGenerator getGenerator() throws ReflectiveOperationException {
        return generatorClass.getDeclaredConstructor().newInstance();
    }

    public String getName() {
        return name;
    }

    public String[] getDocTypes() {
        return docTypes;
    }

    public int getCounterStart() {
        return counterStart;
    }

    public String[] getPropertyNames() {
        return propertyNames;
    }

}
