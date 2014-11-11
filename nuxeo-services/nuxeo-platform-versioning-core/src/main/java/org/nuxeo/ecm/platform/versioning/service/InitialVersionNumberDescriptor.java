/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:bjalon@nuxeo.com">Benjamin JALON</a>
 *
 * $Id: VersioningPropertiesDescriptor.java 20597 2007-06-16 16:50:37Z sfermigier $
 */

package org.nuxeo.ecm.platform.versioning.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor to define what fields to use for versioning given a document type.
 *
 * @author <a href="mailto:bjalon@nuxeo.com">Benjamin JALON</a>
 */
@XObject("initialVersionNumber")
public class InitialVersionNumberDescriptor {

    @XNode("majorVersion")
    private String majorVersion;

    @XNode("minorVersion")
    private String minorVersion;

    public String getMajorVersion() {
        return majorVersion;
    }

    public String getMinorVersion() {
        return minorVersion;
    }

}
