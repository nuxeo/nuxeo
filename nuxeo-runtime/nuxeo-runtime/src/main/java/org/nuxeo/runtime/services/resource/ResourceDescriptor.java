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
 */
package org.nuxeo.runtime.services.resource;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.nuxeo.common.xmap.Resource;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * A pointer to a template located in the contributor bundle.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("resource")
public class ResourceDescriptor {

    @XNode("@name")
    protected String name;

    @XNode
    protected Resource resource;

    public ResourceDescriptor() {

    }

    public ResourceDescriptor(String name, Resource resource) {
        this.name = name;
        this.resource = resource;
    }

    public Resource getResource() {
        return resource;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public URL getUrl() {
        return resource.toURL();
    }

    public URI getUri() throws URISyntaxException {
        return resource.toURI();
    }

    public File getFile() throws URISyntaxException {
        return resource.toFile();
    }

    @Override
    public String toString() {
        return name + "@" + resource.toURL();
    }

}
