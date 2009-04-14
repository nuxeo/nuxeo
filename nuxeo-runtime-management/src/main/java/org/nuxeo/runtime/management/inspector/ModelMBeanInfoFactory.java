/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.runtime.management.inspector;

import java.util.HashMap;
import java.util.Map;

import javax.management.modelmbean.ModelMBeanInfo;

public class ModelMBeanInfoFactory {

    private final Map<Class<?>, ModelMBeanInfo> infos = new HashMap<Class<?>, ModelMBeanInfo>();

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
