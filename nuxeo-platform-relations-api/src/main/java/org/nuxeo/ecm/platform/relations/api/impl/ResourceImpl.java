/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 */
public class ResourceImpl extends AbstractNode implements Resource {

    private static final long serialVersionUID = 1L;

    protected String uri;

    public ResourceImpl() {
    }

    public ResourceImpl(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public NodeType getNodeType() {
        return NodeType.RESOURCE;
    }

    @Override
    public boolean isResource() {
        return true;
    }

    @Override
    public String toString() {
        return String.format("<%s '%s'>", getClass(), uri);
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
        return uri == null ? otherResource.uri == null : uri
                .equals(otherResource.uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

}
