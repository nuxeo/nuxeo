/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.common.server;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("webapp")
public class WebApplication {

    @XNode("root")
    protected String root;

    @XNode("webXml")
    protected String webXml;

    /**
     * @since 10.2
     */
    @XNode("@context")
    protected String context;

    // compat
    @XNode("@path")
    protected void setPath(String path) {
        this.context = path;
    }

    public String getWebRoot() {
        return root;
    }

    public String getConfigurationFile() {
        return webXml;
    }

    public String getContextPath() {
        return context;
    }

}
