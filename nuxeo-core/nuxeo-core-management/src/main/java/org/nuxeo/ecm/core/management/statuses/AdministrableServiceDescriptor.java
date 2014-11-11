/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
