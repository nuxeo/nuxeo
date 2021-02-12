/*

 * (C) Copyright 2015-2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 *     Stephane Lacoin <slacoin@nuxeo.com>
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 *
 */
package org.nuxeo.automation.scripting.internals;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.AbstractRegistry;
import org.w3c.dom.Element;

/**
 * Registry for {@link ClassFilterDescriptor}.
 *
 * @since 11.5
 */
public class ClassFilterRegistry extends AbstractRegistry {

    protected Set<String> allowedClassNames = new HashSet<>();

    @Override
    public void initialize() {
        allowedClassNames.clear();
        super.initialize();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T doRegister(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        ClassFilterDescriptor contrib = getInstance(ctx, xObject, element);
        if (contrib.deny.contains("*")) {
            allowedClassNames.clear();
            allowedClassNames.addAll(contrib.allow);
        } else {
            allowedClassNames.addAll(contrib.allow);
            allowedClassNames.removeAll(contrib.deny);
        }
        return (T) contrib;
    }

    public Set<String> getAllowedClassNames() {
        return Collections.unmodifiableSet(allowedClassNames);
    }

}
