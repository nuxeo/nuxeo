/*
 * (C) Copyright 2013-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     slacoin
 */
package org.nuxeo.dmk;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;

@XObject("protocol")
public class DmkProtocol {

    @XNode("@name")
    public String name = "html";

    @XNode("port")
    public int port = 8081;

    @XNode("user")
    public String user = "operator";

    @XNode("password")
    public String password = Framework.getProperty("server.status.key", "pfouh");

    @Override
    public String toString() {
        return "DmkProtocol [name=" + name + ", port=" + port + ", user="
                + user + "]";
    }

}
