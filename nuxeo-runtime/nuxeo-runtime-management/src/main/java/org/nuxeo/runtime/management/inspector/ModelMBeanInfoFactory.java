/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.runtime.management.inspector;

import java.util.HashMap;
import java.util.Map;

import javax.management.modelmbean.ModelMBeanInfo;

public class ModelMBeanInfoFactory {

    private final Map<Class<?>, ModelMBeanInfo> infos = new HashMap<>();

    public ModelMBeanInfoFactory() {
        super(); // enabled breaking
    }

    public ModelMBeanInfo getModelMBeanInfo(Class<?> resourceClass) {
        if (infos.containsKey(resourceClass)) {
            return infos.get(resourceClass);
        }
        ModelMBeanInfo info = new ModelMBeanIntrospector(resourceClass).introspect();
        infos.put(resourceClass, info);
        return info;
    }

}
