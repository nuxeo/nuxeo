/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.context;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 7.3
 */
@XObject("contextHelper")
public class ContextHelperDescriptor {

    @XNode("@id")
    protected String id;

    protected ContextHelper contextHelper;

    @XNode("@class")
    public void setClass(Class<? extends ContextHelper> aType) throws InstantiationException, IllegalAccessException {
        contextHelper = aType.newInstance();
    }

    @XNode("@enabled")
    protected boolean enabled = true;

    public ContextHelper getContextHelper() {
        return contextHelper;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getId() {
        return id;
    }

    public ContextHelperDescriptor clone() {
        ContextHelperDescriptor copy = new ContextHelperDescriptor();
        copy.id = id;
        copy.contextHelper = contextHelper;
        copy.enabled = enabled;
        return copy;
    }

    public void merge(ContextHelperDescriptor src) {
        if (src.contextHelper != null) {
            contextHelper = src.contextHelper;
        }
        enabled = src.enabled;
    }

}
