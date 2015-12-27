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
package org.nuxeo.runtime.jetty;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("servlet")
public class ServletDescriptor {

    // the filter name if any
    @XNode("@name")
    protected String name;

    @XNode("@class")
    protected Class<?> clazz;

    @XNode("@context")
    protected String context;

    @XNode("@path")
    protected String path;

    @XNodeMap(value = "init-params/param", key = "@name", type = HashMap.class, componentType = String.class, trim = true, nullByDefault = true)
    protected Map<String, String> initParams;

    // the description if any
    @XNode("description")
    protected String description;

    public ServletDescriptor() {
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String getContext() {
        return context;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getInitParams() {
        return initParams;
    }

    public void setInitParams(Map<String, String> initParams) {
        this.initParams = initParams;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
