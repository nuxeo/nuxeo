/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.remoting.restHandler;

import org.nuxeo.ecm.platform.publisher.api.PublicationNode;

import javax.ws.rs.core.MediaType;
import java.util.List;

public class RemotePubParam {

    public static final MediaType mediaType = new MediaType("nuxeo",
            "remotepub");

    protected List<Object> params;

    public RemotePubParam(List<Object> params) {
        this.params = params;
    }

    public List<Object> getParams() {
        return params;
    }

    public PublicationNode getAsNode() {
        if (params.size() == 1) {
            return (PublicationNode) params.get(0);
        } else {
            return null;
        }
    }

}
