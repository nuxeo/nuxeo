/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.work.api;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.work.WorkQueuing;

/**
 * Descriptor for a {@link WorkManager} queuing implementation configuration.
 *
 * @since 5.8
 */
@XObject("queuing")
public class WorkQueuingImplDescriptor {

    @XNode("@class")
    public Class<?> klass;

    @SuppressWarnings("unchecked")
    public Class<? extends WorkQueuing> getWorkQueuingClass() {
        if (!(WorkQueuing.class.isAssignableFrom(klass))) {
            throw new RuntimeException("Invalid class: " + klass.getName());
        }
        return (Class<? extends WorkQueuing>) klass;
    }

}
