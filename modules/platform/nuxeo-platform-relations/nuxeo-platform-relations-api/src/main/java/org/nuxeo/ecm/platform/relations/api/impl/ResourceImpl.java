/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: ResourceImpl.java 20796 2007-06-19 09:52:03Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api.impl;

import org.nuxeo.ecm.platform.relations.api.NodeType;
import org.nuxeo.ecm.platform.relations.api.Resource;

/**
 * Resource.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class ResourceImpl extends AbstractNode implements Resource {

    private static final long serialVersionUID = 1L;

    protected String uri;

    public ResourceImpl() {
    }

    public ResourceImpl(String uri) {
        this.uri = uri;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.RESOURCE;
    }

    @Override
    public boolean isResource() {
        return true;
    }

    @Override
    public String toString() {
        return String.format("%s('%s')", getClass().getSimpleName(), uri);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ResourceImpl)) {
            return false;
        }
        ResourceImpl otherResource = (ResourceImpl) other;
        return uri == null ? otherResource.uri == null : uri.equals(otherResource.uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

}
