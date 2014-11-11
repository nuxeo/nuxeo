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
@XmlRootElement(name = "instance")
public class InstanceInfo {

    public InstanceInfo() {
    }

    @XmlElement(name = "NUXEO_CONF")
    public String NUXEO_CONF;

    @XmlElement(name = "NUXEO_HOME")
    public String NUXEO_HOME;

    @XmlElement(name = "clid")
    public String clid;

    @XmlElement(name = "distribution")
    public DistributionInfo distribution;

    @XmlElementWrapper(name = "packages")
    @XmlElement(name = "package")
    public List<PackageInfo> packages = new ArrayList<PackageInfo>();

    @XmlElement(name = "configuration")
    public ConfigurationInfo config;

}
