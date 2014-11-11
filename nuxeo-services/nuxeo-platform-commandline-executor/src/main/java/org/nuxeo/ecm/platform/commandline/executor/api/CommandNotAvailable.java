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

/**
 * Exception indicating that the target command is not available:
 * <ul>
 * <li>because it was never registered,
 * <li>because it was disabled,
 * <li>because the target command is not installed on the server *.
 * </ul>
 *
 * @author tiry
 */
public class CommandNotAvailable extends Exception {

    private static final long serialVersionUID = 1L;

    protected final CommandAvailability availability;

    public CommandNotAvailable(CommandAvailability availability) {
        this.availability = availability;
    }

    public String getInstallMessage() {
        return availability.getInstallMessage();
    }

    public String getErrorMessage() {
        return availability.getErrorMessage();
    }

}
