/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Wojciech Sulejman
 */


package org.nuxeo.ecm.platform.signature.core.sign;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 *  @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */

@XObject("configuration")
public class SignatureDescriptor {

    @XNode("reason")
    protected String reason;

    private boolean remove;

    @XNode("removeExtension")
    protected void setRemoveExtension(boolean remove) {
        this.remove = remove;
    }

    public boolean getRemoveExtension() {
        return remove;
    }

}
