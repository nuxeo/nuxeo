/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
            subtype = mimeType.substring(p + 1);
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
