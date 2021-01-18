/*
 * (C) Copyright 2009-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *    Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.api;

import java.net.URL;

import org.nuxeo.common.xmap.Resource;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * @since 5.6
 */
@XObject("template-resource")
@XRegistry(compatWarnOnMerge = true)
public class RouteModelResourceType {

    @XNode("@id")
    @XRegistryId
    protected String id;

    @XNode("@path")
    protected Resource path;

    @XNode("@path")
    protected String pathh;

    public String getId() {
        return id;
    }

    public URL getUrl() {
        return path.toURL();
    }

    public String getPath() {
        return pathh;
    }

}
