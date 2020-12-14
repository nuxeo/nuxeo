/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.redis;

import java.util.Optional;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.SingleRegistry;
import org.w3c.dom.Element;

/**
 * Single registry without merge or enablement.
 * <p>
 * Allows setting the contribution programatically for tests.
 *
 * @since 11.5
 */
public class RedisPoolRegistry extends SingleRegistry {

    protected RedisPoolDescriptor config;

    @Override
    public void register(Context ctx, XAnnotatedObject xObject, Element element) {
        setContribution(xObject.newInstance(ctx, element));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getContribution() {
        if (config != null) {
            return Optional.of((T) config);
        }
        return super.getContribution();
    }

    public void setContribution(RedisPoolDescriptor config) {
        this.config = config;
    }

}
