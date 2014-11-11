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
 * Custom output field tag descriptor. This tag can specify the outcome
 * parameter names received from a transformation. Also defines the name of the
 * document properties that will be filled with the values corresponding to
 * outcome parameters. The values are are resulted from the transformation
 * process.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
@XObject("docModifier")
public class CustomOutputField {

    @XNode("@name")
    private String name;

    @XNode("@outputParamName")
    private String outputParamName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOutputParamName() {
        return outputParamName;
    }

    public void setOutputParamName(String outputParamName) {
        this.outputParamName = outputParamName;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(CustomOutputField.class.getSimpleName());
        buf.append(" {name=");
        buf.append(name);
        buf.append(", outputParamName=");
        buf.append(outputParamName);
        buf.append('}');

        return buf.toString();
    }

}
