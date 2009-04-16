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
 * $Id: MimetypeDescriptor.java 20310 2007-06-11 15:54:14Z lgodard $
 */
package org.nuxeo.ecm.platform.mimetype.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Filename extension definition.
 * <p>
 * Allow the mimetype service to guess which mimetype to use for each extension.
 * Ambiguous extensions (such as xml) tell the service that a binary sniffing
 * operation is advised to guess the right mimetype.
 *
 * @author <a href="mailto:og@nuxeo.com">Olivier Grisel</a>
 */
@XObject("fileExtension")
public class ExtensionDescriptor {

    @XNode("@name")
    protected String name;

    @XNode("@mimetype")
    protected String mimetype;

    protected boolean ambiguous = false;


    public ExtensionDescriptor() { }

    public ExtensionDescriptor(String name) {
        this.name = name;
    }

    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public boolean isAmbiguous() {
        return ambiguous;
    }

    @XNode("@ambiguous")
    public void setAmbiguous(boolean ambiguous) {
        this.ambiguous = ambiguous;
    }

    public String getName() {
        return name;
    }

}
