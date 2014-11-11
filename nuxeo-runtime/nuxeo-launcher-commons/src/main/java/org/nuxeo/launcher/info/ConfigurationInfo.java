/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mguillaume
 */

package org.nuxeo.launcher.info;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "configuration")
public class ConfigurationInfo {

    public ConfigurationInfo() {
    }

    @XmlElement(name = "dbtemplate")
    public String dbtemplate = "default";

    @XmlElementWrapper(name = "basetemplates")
    @XmlElement(name = "template")
    public List<String> basetemplates = new ArrayList<String>();

    @XmlElementWrapper(name = "pkgtemplates")
    @XmlElement(name = "template")
    public List<String> pkgtemplates = new ArrayList<String>();

    @XmlElementWrapper(name = "usertemplates")
    @XmlElement(name = "template")
    public List<String> usertemplates = new ArrayList<String>();

    @XmlElementWrapper(name = "keyvals")
    @XmlElement(name = "keyval")
    public List<KeyValueInfo> keyvals = new ArrayList<KeyValueInfo>();

}
