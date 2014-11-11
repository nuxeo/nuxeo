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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.webengine.gadgets;

import java.net.URL;

import javax.ws.rs.GET;

import org.nuxeo.opensocial.container.service.ContainerServiceImpl;

public class UrlResource extends InputStreamResource {

    private String path;

    public UrlResource(String path) {
        this.path = path;
    }

    @GET
    public Object getGadgetFile() throws Exception {

        URL url = ContainerServiceImpl.class.getResource(path);

        return url == null ? null : getObject(url.openStream(), path);

    }
}
