/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.schema.types.resolver;

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
