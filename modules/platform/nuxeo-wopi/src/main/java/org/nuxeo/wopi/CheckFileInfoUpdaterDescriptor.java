/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.wopi;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 2021.40
 */
@XObject("checkFileInfoUpdater")
public class CheckFileInfoUpdaterDescriptor implements Descriptor {

    @XNode("@class")
    public Class<? extends CheckFileInfoUpdater> klass;

    @Override
    public String getId() {
        return UNIQUE_DESCRIPTOR_ID;
    }

    public CheckFileInfoUpdater newInstance() {
        if (!CheckFileInfoUpdater.class.isAssignableFrom(klass)) {
            throw new IllegalArgumentException(
                    "Cannot instantiate class: " + klass + ", class must implement CheckFileInfoUpdater");
        }
        try {
            return klass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Cannot instantiate: " + klass, e);
        }
    }

}
