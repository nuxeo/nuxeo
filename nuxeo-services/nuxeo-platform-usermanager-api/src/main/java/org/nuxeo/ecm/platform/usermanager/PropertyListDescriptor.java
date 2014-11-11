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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: PropertyListDescriptor.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.usermanager;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Property list descriptor
 *
 *APG-240 All attributes are defined public because the user manager service do not get
 * access to the fields. OSGI don't allow splitted packages having access to public members defined
 * from an another package provider.
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@XObject("propertyList")
public class PropertyListDescriptor implements Serializable {

    private static final long serialVersionUID = -5870562997550545838L;

    @XNodeList(value = "value", type = String[].class, componentType = String.class)
    public String[] values = new String[0];

    public String[] getValues() {
        return values;
    }

}
