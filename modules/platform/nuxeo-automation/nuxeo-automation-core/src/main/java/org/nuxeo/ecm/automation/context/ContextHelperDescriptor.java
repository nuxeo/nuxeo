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
    public void setClass(Class<? extends ContextHelper> aType) throws ReflectiveOperationException {
        contextHelper = aType.getDeclaredConstructor().newInstance();
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

    @Override
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
