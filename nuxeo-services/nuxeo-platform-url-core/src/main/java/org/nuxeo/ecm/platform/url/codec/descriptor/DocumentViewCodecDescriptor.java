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
 * $Id: DocumentViewCodecDescriptor.java 22535 2007-07-13 14:57:58Z atchertchian $
 */

package org.nuxeo.ecm.platform.url.codec.descriptor;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject(value = "documentViewCodec")
public class DocumentViewCodecDescriptor {

    @XNode("@name")
    private String name;

    @XNode("@class")
    private String className;

    @XNode("@prefix")
    private String prefix;

    @XNode("@default")
    private boolean defaultCodec;

    @XNode("@enabled")
    private boolean enabled;

    public String getClassName() {
        return className;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public String getName() {
        return name;
    }

    public boolean getDefaultCodec() {
        return defaultCodec;
    }

    public String getPrefix() {
        return prefix;
    }

}
