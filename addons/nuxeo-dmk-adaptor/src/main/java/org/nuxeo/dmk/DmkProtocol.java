/*
 * (C) Copyright 2013-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     slacoin
 */
package org.nuxeo.dmk;

import org.nuxeo.common.Environment;
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
    public String password = Framework.getProperty(Environment.SERVER_STATUS_KEY);

    @Override
    public String toString() {
        return "DmkProtocol [name=" + name + ", port=" + port + ", user=" + user + "]";
    }

}
