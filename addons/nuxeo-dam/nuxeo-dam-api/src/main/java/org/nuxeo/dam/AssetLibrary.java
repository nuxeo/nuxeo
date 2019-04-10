/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.dam;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Object holding information about the Asset Library document
 * where the assets will be stored after importing.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@XObject("assetLibrary")
public class AssetLibrary {

    @XNode("@title")
    private String title;

    @XNode("@description")
    private String description;

    @XNode("@path")
    private String path;

    /**
     * Returns the configured title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the configured description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the path.
     */
    public String getPath() {
        return path;
    }

}
