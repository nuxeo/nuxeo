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

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * UID generator configuration holder.
 */
@XObject("generator")
public class UIDGeneratorDescriptor {

    private static final Log log = LogFactory.getLog(UIDGeneratorDescriptor.class);

    private static final int DEFAULT_COUNTER_START = 1;

    // @XNode
    private String generationExpression;

    // @XNode
    private Set<?> generationCriteria;

    // @XNode
    private int counterStart;

    @XNode("@name")
    private String name;

    @XNode("@class")
    private String className;

    // @XNode("propertyName")
    // private String propertyName;
    @XNodeList(value = "propertyName", type = String[].class, componentType = String.class)
    private String[] propertyNames;

    @XNodeList(value = "docType", type = String[].class, componentType = String.class)
    private String[] docTypes;

    /**
     * Default constructor - used normally when created as an XObject.
     */
    public UIDGeneratorDescriptor() {
        log.debug("<UIDGeneratorDescriptor:init>");
    }

    /**
     * Explicit constructor.
     */
    public UIDGeneratorDescriptor(String generationExp, Set<?> generationCrit) {
        this(generationExp, generationCrit, DEFAULT_COUNTER_START);
    }

    /**
     * Explicit constructor.
     */
    public UIDGeneratorDescriptor(String generationExp, Set<?> generationCrit, int counterStart) {
        generationExpression = generationExp;
        generationCriteria = generationCrit;
        this.counterStart = counterStart;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getDocTypes() {
        return docTypes;
    }

    public void setDocTypes(String[] docTypes) {
        this.docTypes = docTypes;
    }

    public int getCounterStart() {
        return counterStart;
    }

    public Set<?> getGenerationCriteria() {
        return generationCriteria;
    }

    public String getGenerationExpression() {
        return generationExpression;
    }

    /**
     * Kept for convenience. If there is only one property to be set with generated UID.
     *
     * @return first propertyName
     */
    public String getPropertyName() {
        if (propertyNames.length == 0) {
            log.warn("No propertyName specified");
            return null;
        }
        return propertyNames[0];
    }

    /**
     * Set the value as first property name. Kept for convenience. If there is only one property to be set with
     * generated UID.
     */
    public void setPropertyName(String propertyName) {
        if (propertyNames.length == 0) {
            log.warn("Cannot set propertyName.");
        }
        propertyNames[0] = propertyName;
    }

    public String[] getPropertyNames() {
        return propertyNames;
    }

    public void setPropertyNames(String[] propNames) {
        propertyNames = propNames;
    }

}
