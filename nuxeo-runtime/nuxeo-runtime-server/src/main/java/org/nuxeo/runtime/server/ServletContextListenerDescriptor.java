/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.runtime.server;

import javax.servlet.ServletContextListener;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("listener")
public class ServletContextListenerDescriptor {

    @XNode("@name")
    protected String name;

    /**
     * @since 10.2
     */
    @XNode("listener-class")
    protected Class<? extends ServletContextListener> clazz;

    // compat
    @XNode("@class")
    public void setClass(Class<? extends ServletContextListener> clazz) {
        this.clazz = clazz;
    }

    @XNode("@context")
    protected String context = "/";

    public Class<? extends ServletContextListener> getClazz() {
        return clazz;
    }

    public String getContext() {
        return context;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
