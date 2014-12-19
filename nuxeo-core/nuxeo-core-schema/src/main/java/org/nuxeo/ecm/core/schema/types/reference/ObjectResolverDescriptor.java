/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.schema.types.reference;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Handler for the {@link ObjectResolverService} "resolvers" extension point.
 *
 * @since 7.1
 */
@XObject("resolver")
public class ObjectResolverDescriptor {

    @XNode("@type")
    private String type;

    @XNode("@class")
    private Class<? extends ObjectResolver> resolver;

    public ObjectResolverDescriptor() {
    }

    public ObjectResolverDescriptor(String type, Class<? extends ObjectResolver> resolver) {
        super();
        this.type = type;
        this.resolver = resolver;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Class<? extends ObjectResolver> getResolver() {
        return resolver;
    }

    public void setResolver(Class<? extends ObjectResolver> resolver) {
        this.resolver = resolver;
    }

    @Override
    public String toString() {
        return type + ": " + resolver.getCanonicalName();
    }

}
