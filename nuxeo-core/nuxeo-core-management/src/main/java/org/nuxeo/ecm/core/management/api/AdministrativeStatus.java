/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.management.api;

import java.io.Serializable;
import java.util.Calendar;

/**
 * Representation of the Administrative Status of a service or server
 *
 * @author tiry
 */
public class AdministrativeStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ACTIVE = "active";

    public static final String PASSIVE = "passive";

    protected String state;

    protected String message;

    protected Calendar modificationDate;

    protected String userLogin;

    protected String instanceIdentifier;

    protected String serviceIdentifier;

    protected String label;

    protected String description;

    public AdministrativeStatus(String state, String message,
            Calendar modificationDate, String userLogin,
            String instanceIdentifier, String serviceIdentifier) {
        this.state = state;
        this.message = message;
        this.modificationDate = modificationDate;
        this.userLogin = userLogin;
        this.instanceIdentifier = instanceIdentifier;
        this.serviceIdentifier = serviceIdentifier;
    }

    public void setLabelAndDescription(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getInstanceIdentifier() {
        return instanceIdentifier;
    }

    public String getServiceIdentifier() {
        return serviceIdentifier;
    }

    public String getState() {
        return state;
    }

    public String getMessage() {
        return message;
    }

    public Calendar getModificationDate() {
        return modificationDate;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public boolean isActive() {
        return state.equals(ACTIVE);
    }

    public boolean isPassive() {
        return state.equals(PASSIVE);
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return String.format("administrativeStatus(%s,%s)", instanceIdentifier,
                state);
    }

}
