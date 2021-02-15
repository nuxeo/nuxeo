/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.webengine.rendering;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("rendering-extension")
@XRegistry
public class RenderingExtensionDescriptor {

    @XNode("@name")
    @XRegistryId
    public String name;

    @XNode("@class")
    public Class<?> klass;

    public Object newInstance() throws ReflectiveOperationException {
        Object obj = klass.getDeclaredConstructor().newInstance();
        if (obj instanceof RenderingExtensionFactory) {
            obj = ((RenderingExtensionFactory) obj).createTemplate();
        }
        return obj;
    }

}
