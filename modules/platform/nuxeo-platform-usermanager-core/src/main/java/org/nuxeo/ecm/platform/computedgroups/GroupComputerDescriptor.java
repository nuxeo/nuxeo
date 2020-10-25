/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 * *
 */

package org.nuxeo.ecm.platform.computedgroups;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * @author Thierry Delprat
 */
@XObject("groupComputer")
public class GroupComputerDescriptor {

    @XNode("computer")
    protected Class<GroupComputer> computerClass;

    protected GroupComputer groupComputer;

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected boolean enabled = true;

    public String getName() {
        if (name != null) {
            return name;
        }
        return computerClass.getSimpleName();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public GroupComputer getComputer() {
        if (groupComputer == null) {
            if (computerClass != null) {
                try {
                    groupComputer = computerClass.getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new NuxeoException(e);
                }
            } else {
                groupComputer = null;
            }
        }
        return groupComputer;
    }

}
