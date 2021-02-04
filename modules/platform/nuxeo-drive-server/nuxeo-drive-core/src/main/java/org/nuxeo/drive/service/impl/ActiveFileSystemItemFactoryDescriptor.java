/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * XMap descriptor for the {@code factory} elements of the {@code activeFileSystemItemFactories} contributions.
 *
 * @author Antoine Taillefer
 */
@XObject("factory")
public class ActiveFileSystemItemFactoryDescriptor {

    @XNode
    @XRegistryId
    protected String name;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled", defaultAssignment = "true")
    protected boolean enabled;

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        sb.append(" (enabled = ");
        sb.append(enabled);
        sb.append(")");
        return sb.toString();
    }

}
