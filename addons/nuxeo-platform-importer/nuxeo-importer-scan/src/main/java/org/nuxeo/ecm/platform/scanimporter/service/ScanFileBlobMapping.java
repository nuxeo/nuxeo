/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */
package org.nuxeo.ecm.platform.scanimporter.service;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 *
 * XMap descriptor for Blob mapping
 *
 * @author Thierry Delprat
 *
 */
@XObject("blobMapping")
public class ScanFileBlobMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@sourceXPath")
    protected String sourceXPath;

    @XNode("@sourcePathAttribute")
    protected String sourcePathAttribute;

    @XNode("@sourceFilenameAttribute")
    protected String sourceFilenameAttribute;

    @XNode("@targetXPath")
    protected String targetXPath;

    public String getSourceXPath() {
        return sourceXPath;
    }

    public String getTargetXPath() {
        return targetXPath;
    }

    public String getSourcePathAttribute() {
        return sourcePathAttribute;
    }

    public String getSourceFilenameAttribute() {
        return sourceFilenameAttribute;
    }

}
