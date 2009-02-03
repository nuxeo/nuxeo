/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 *
 */

package org.nuxeo.ecm.platform.commandline.executor.api;

import java.io.Serializable;

/**
 * Represents the availability status of a command.
 * If command is not available, {@link CommandAvailability}
 * contains the errorMessage and some installation instructions.
 *
 * @author tiry
 */
public class CommandAvailability implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String installMessage;

    protected String errorMessage;

    protected final boolean available;


    public CommandAvailability() {
        available = true;
    }

    public CommandAvailability(String errorMessage) {
        available = false;
        this.errorMessage = errorMessage;
    }

    public CommandAvailability(String installMessage, String errorMessage) {
        available = false;
        this.installMessage = installMessage;
        this.errorMessage = errorMessage;
    }

    public String getInstallMessage() {
        return installMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isAvailable() {
        return available;
    }

}
