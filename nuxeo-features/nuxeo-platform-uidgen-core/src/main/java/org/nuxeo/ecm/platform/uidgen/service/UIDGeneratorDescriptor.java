/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.uidgen.service;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * immutable?
 *
 * @author dragos
 *
 */
/**
 * @author dragos
 *
 */
@XObject("generator")
public class UIDGeneratorDescriptor {

    private static final Log log = LogFactory.getLog(UIDGeneratorDescriptor.class);

    private static final int DEFAULT_COUNTER_START = 1;

    // @XNode
    private String generationExpression;

    // @XNode
    private Set generationCriteria;

    // @XNode
    private int counterStart;

    @XNode("@name")
    private String name;

    @XNode("@class")
    private String className;

    @XNode("propertyName")
    private String propertyName;

    @XNodeList(value = "docType", type = String[].class, componentType = String.class)
    private String[] docTypes;

    /**
     * Default constructor - used normally when created as an XObject.
     *
     */
    public UIDGeneratorDescriptor() {
        log.debug("<UIDGeneratorDescriptor:init>");
    }

    /**
     * Explicit constructor.
     *
     * @param generationExp
     * @param generationCrit
     */
    public UIDGeneratorDescriptor(String generationExp, Set generationCrit) {
        this(generationExp, generationCrit, DEFAULT_COUNTER_START);
    }

    /**
     * Explicit constructor.
     *
     * @param generationExp
     * @param generationCrit
     */
    public UIDGeneratorDescriptor(String generationExp, Set generationCrit,
            int counterStart) {
        this.generationExpression = generationExp;
        this.generationCriteria = generationCrit;
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

    public Set getGenerationCriteria() {
        return generationCriteria;
    }

    public String getGenerationExpression() {
        return generationExpression;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

}
