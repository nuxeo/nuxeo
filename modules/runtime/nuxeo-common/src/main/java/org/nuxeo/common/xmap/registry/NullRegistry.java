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
package org.nuxeo.common.xmap.registry;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.w3c.dom.Element;

/**
 * Null registry for backward compatibility management.
 * <p>
 * Allows setting a non-null registry to avoid repeated lookups, while no registry is defined.
 *
 * @since 11.5
 */
public class NullRegistry extends AbstractRegistry implements Registry {

    public NullRegistry() {
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    protected void register(Context ctx, XAnnotatedObject xObject, Element element) {
        // NOOP
    }

}
