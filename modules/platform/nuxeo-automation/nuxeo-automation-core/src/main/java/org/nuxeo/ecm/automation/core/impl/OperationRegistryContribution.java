/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.automation.core.impl;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.RegistryContribution;
import org.w3c.dom.Element;

/**
 * Wrapper for {@link RegistryContribution} where contribution is already instantiated: useful for scripting operation
 * registration.
 *
 * @since 11.5
 */
public class OperationRegistryContribution extends RegistryContribution {

    protected final String id;

    protected final boolean replace;

    protected final String[] aliases;

    protected final Object instance;

    public OperationRegistryContribution(Context context, XAnnotatedObject object, Element element, String tag,
            String id, boolean replace, String[] aliases, Object instance) {
        super(context, object, element, tag);
        this.id = id;
        this.replace = replace;
        this.aliases = aliases;
        this.instance = instance;
    }

    public String getId() {
        return id;
    }

    public boolean isReplace() {
        return replace;
    }

    public String[] getAliases() {
        return aliases;
    }

    public Object getInstance() {
        return instance;
    }

}
