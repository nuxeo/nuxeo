/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 *
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
