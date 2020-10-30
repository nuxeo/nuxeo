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
 *     Benjamin Jalon
 */

package org.nuxeo.ecm.platform.computedgroups;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 5.7.3
 */
@XObject("userMetadataGroupComputer")
public class UserMetadataGroupComputerDescriptor extends GroupComputerDescriptor {

    @XNode("@xpath")
    public String xpath;

    @XNode("@groupPattern")
    public String groupPattern = "%s";

    @XNode("@name")
    public String name;

    @XNode("@enabled")
    public boolean enabled = true;

    @Override
    public String getName() {
        if (name != null) {
            return name;
        }
        return computerClass.getSimpleName();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public GroupComputer getComputer() {
        return new UserMetadataGroupComputer(xpath, groupPattern);
    }

}
