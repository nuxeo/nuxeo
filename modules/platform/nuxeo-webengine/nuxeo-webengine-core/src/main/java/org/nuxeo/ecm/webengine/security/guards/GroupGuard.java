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
 * $Id$
 */

package org.nuxeo.ecm.webengine.security.guards;

import java.security.Principal;

import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.runtime.model.Adaptable;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("group")
public class GroupGuard implements Guard {

    @XContent
    protected String group;

    protected GroupGuard() {
    }

    public GroupGuard(String group) {
        this.group = group;
    }

    @Override
    public boolean check(Adaptable context) {
        Principal p = context.getAdapter(Principal.class);
        if (p instanceof NuxeoPrincipal) {
            return ((NuxeoPrincipal) p).isMemberOf(group);
        }
        return false;
    }

    @Override
    public String toString() {
        return "GROUP[" + group + "]";
    }

}
