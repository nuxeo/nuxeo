/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.io.registry;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * POJO used to handle "register" {@link MarshallerRegistry} extension point.
 *
 * @since 7.2
 */
@XObject("register")
public class MarshallerRegistryDescriptor {

    @XNode("@class")
    private Class<?> clazz;

    @XNode("@enable")
    private boolean enable;

    public MarshallerRegistryDescriptor() {
    }

    public MarshallerRegistryDescriptor(Class<?> clazz, boolean enable) {
        super();
        this.clazz = clazz;
        this.enable = enable;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    @Override
    public String toString() {
        return clazz.getName() + ":" + Boolean.toString(enable);
    }

}
