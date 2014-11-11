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

package org.nuxeo.ecm.platform.modifier.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 *
 * @author DM
 */

@XObject("docModifier")
public class CustomField {

    @XNode("@name")
    private String name;

    @XNode("@value")
    private String value;

    @XNode("@transformParamName")
    private String transformParamName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTransformParamName() {
        return transformParamName;
    }

    public void setTransformParamName(String transformParamName) {
        this.transformParamName = transformParamName;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(CustomField.class.getSimpleName());
        buf.append(" {name=");
        buf.append(name);
        buf.append(", value=");
        buf.append(value);
        buf.append(", transformParamName=");
        buf.append(transformParamName);
        buf.append('}');

        return buf.toString();
    }
}
