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
 * $Id: TransformerExtension.java 7356 2006-12-05 16:08:23Z janguenot $
 */

package org.nuxeo.ecm.platform.modifier.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 *
 * @author DM
 */
@XObject("docModifier")
public class DocModifierEPDescriptor {

    @XNode("@name")
    private String name;

    @XNode("@documentType")
    private String documentType;

    @XNode("@transformationPluginName")
    private String pluginName;

    @XNode("@sourceFieldName")
    private String srcFieldName;

    @XNode("@destinationFieldName")
    private String destFieldName;

    /**
     * Defaults to 100.
     */
    @XNode("@order")
    private int order = 100;

    @XNodeList(value = "docType", type = String[].class, componentType = String.class)
    private String[] documentTypes;

    @XNodeList(value = "customField", type = CustomField[].class, componentType = CustomField.class)
    private CustomField[] customFields;

    @XNodeList(value = "customOutputField", type = CustomOutputField[].class, componentType = CustomOutputField.class)
    private CustomOutputField[] customOutputFields;

    /**
     * The core Events that will trigger a Document modification process.
     */
    @XNodeList(value = "coreEvent", type = String[].class, componentType = String.class)
    private String[] coreEvents;

    public String[] getDocumentTypes() {
        // TODO check the rule for field value injection (null vs empty str)
        if (documentTypes != null && documentTypes.length != 0) {
            return documentTypes;
        }
        return new String[] { documentType };
    }

    public String getName() {
        return name;
    }

    public String getPluginName() {
        return pluginName;
    }

    public int getOrder() {
        return order;
    }

    public CustomField[] getCustomFields() {
        return customFields;
    }

    public CustomOutputField[] getCustomOutputFields() {
        return customOutputFields;
    }

    public String getSrcFieldName() {
        return srcFieldName;
    }

    public String getDestFieldName() {
        return destFieldName;
    }

    public String[] getCoreEvents() {
        return coreEvents;
    }

}
