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
 *     Benjamin JALON<bjalon@nuxeo.com>
 */
package org.nuxeo.ecm.platform.computedgroups.test;

import org.nuxeo.ecm.platform.computedgroups.GroupComputerLabelled;

/**
 * @since 5.7.3
 */
public class DummyGroupComputerLabelled extends DummyGroupComputer implements GroupComputerLabelled {

    @Override
    public String getLabel(String groupName) {
        if ("Grp1".equals(groupName)) {
            return "Groupe 1";
        }
        if ("Grp2".equals(groupName)) {
            return "Groupe 2";
        }
        return null;
    }
}
