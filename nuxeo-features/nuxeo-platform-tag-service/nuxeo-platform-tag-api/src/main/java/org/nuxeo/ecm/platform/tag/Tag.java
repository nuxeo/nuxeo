/*
 * (C) Copyright 2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.tag;

import java.io.Serializable;

/**
 * Simple class holder for transfer of the Tag id and Label together. It is
 * usually obtained from a query and handled to the caller.
 *
 * @author rux
 */
public class Tag implements Serializable {

    private static final long serialVersionUID = -323612876570705842L;

    /**
     * Tag ID.
     */
    public String tagId;

    /**
     * Tag Label.
     */
    public String tagLabel;

    public Tag(String tagId, String tagLabel) {
        this.tagId = tagId;
        this.tagLabel = tagLabel;
    }

    public Tag() {
    }

}
