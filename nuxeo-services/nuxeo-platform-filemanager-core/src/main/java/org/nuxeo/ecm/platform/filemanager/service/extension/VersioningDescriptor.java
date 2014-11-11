/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thierry Martins
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.filemanager.service.extension;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 5.7
 */

@XObject("versioning")
public class VersioningDescriptor implements Serializable {

    private static final long serialVersionUID = 8615121233156981874L;

    @XNode("defaultVersioningOption")
    public String defaultVersioningOption;

    @XNode("versionAfterAdd")
    public Boolean versionAfterAdd;

}
