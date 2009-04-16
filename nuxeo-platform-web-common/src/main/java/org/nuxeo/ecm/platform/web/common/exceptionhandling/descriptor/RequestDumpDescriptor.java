/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.web.common.exceptionhandling.descriptor;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.service.RequestDumper;

/**
 * @author arussel
 *
 */
@XObject("requestdump")
public class RequestDumpDescriptor {

    @XNode("@class")
    Class<? extends RequestDumper> klass;

    @XNodeList(value = "notListed/attribute", type = ArrayList.class, componentType = String.class)
    List<String> attributes = new ArrayList<String>();

    public Class<? extends RequestDumper> getKlass() {
        return klass;
    }

    public List<String> getAttributes() {
        return attributes;
    }

}
