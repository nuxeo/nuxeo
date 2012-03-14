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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.nuxeo.connect.update.LocalPackage;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "package")
public class PackageInfo {

    public PackageInfo() {
    }

    public PackageInfo(LocalPackage pkg) {
        name = pkg.getName();
        version = pkg.getVersion().toString();
        id = pkg.getId();
        state = pkg.getState();
    }

    @XmlAttribute()
    public String name;

    @XmlAttribute()
    public String version;

    @XmlAttribute()
    public String id;

    @XmlAttribute()
    public int state;

}
