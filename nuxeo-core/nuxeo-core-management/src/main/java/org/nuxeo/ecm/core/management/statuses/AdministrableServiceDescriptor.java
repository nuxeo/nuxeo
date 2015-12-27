/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.management.statuses;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.management.api.AdministrativeStatus;

@XObject("administrableService")
public class AdministrableServiceDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@id")
    private String id;

    @XNode("@name")
    private String name;

    @XNode("description")
    private String description;

    @XNode("label")
    private String label;

    @XNode("initialState")
    private String initialState = AdministrativeStatus.ACTIVE;

    public String getInitialState() {
        return initialState;
    }

    public String getLabel() {
        if (label == null) {
            return "label." + getName();
        }
        return label;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        if (description == null) {
            return getName() + ".description";
        }
        return description;
    }

    public String getName() {
        if (name == null) {
            return id;
        }
        return name;
    }
}
