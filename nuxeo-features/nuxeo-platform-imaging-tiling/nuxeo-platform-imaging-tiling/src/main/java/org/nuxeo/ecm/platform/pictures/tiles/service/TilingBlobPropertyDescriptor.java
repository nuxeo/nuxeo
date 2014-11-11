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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 *
 */
package org.nuxeo.ecm.platform.pictures.tiles.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for Blob property using by tiling for a specific document type.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */

@XObject(value = "blobProperties")
public class TilingBlobPropertyDescriptor implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @XNodeMap(value = "blobProperty", key = "@docType", type = HashMap.class, componentType = String.class)
    public Map<String, String> blobProperties = new HashMap<String, String>();

    public Map<String, String> getBlobProperties() {
        return blobProperties;
    }

}
