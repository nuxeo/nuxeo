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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model.impl;

import javax.ws.rs.core.MediaType;

import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("media-type")
public class MediaTypeRef {

    @XNode("@id")
    public String id;

    public String type;

    public String subtype;

    @XContent
    public void setMimeType(String mimeType) {
        mimeType = mimeType.trim().toLowerCase();
        int p = mimeType.indexOf('/');
        if (p > -1) {
            type = mimeType.substring(0, p);
            subtype = mimeType.substring(p+1);
            if (subtype.length() == 0 || subtype.equals("*")) {
                subtype = "*";
            }
        } else {
            type = mimeType;
            subtype = "*";
        }
        if (type.length() == 0 || type.equals("*")) {
            type = "*";
        }
    }

    public String match(MediaType mt) {
        if (type != "*" && !type.equals(mt.getType())) {
            return null;
        }
        if (subtype != "*" && !subtype.equals(mt.getSubtype())) {
            return null;
        }
        return id;
    }

}
