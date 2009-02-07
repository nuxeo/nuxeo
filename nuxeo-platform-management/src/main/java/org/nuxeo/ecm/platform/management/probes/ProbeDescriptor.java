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
 *     matic
 */
package org.nuxeo.ecm.platform.management.probes;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 *
 */
@XObject("usecase")
public class ProbeDescriptor {

    @XNode("@name")
    private String shortcutName;

    @XNode("@qualifiedName")
    private String qualifiedName;

    @XNode("@serviceClass")
    private Class<?> serviceClass;

    @XNode("@class")
    private Class<? extends Probe> usecaseClass;

    public String getShortcutName() {
        return shortcutName;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public Class<? extends Probe> getProbeClass() {
        return usecaseClass;
    }

    public Class<?> getServiceClass() {
        return serviceClass;
    }

}
