/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Laurent Doguin
 */
package org.nuxeo.ecm.core.versioning;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor to contribute the initial version state of a document.
 * 
 * @author Laurent Doguin
 * @since 5.4.1
 */
@XObject("initialState")
public class InitialStateDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@minor")
    protected int minor = 0;

    @XNode("@major")
    protected int major = 0;

    public int getMinor() {
        return minor;
    }

    public int getMajor() {
        return major;
    }

}
