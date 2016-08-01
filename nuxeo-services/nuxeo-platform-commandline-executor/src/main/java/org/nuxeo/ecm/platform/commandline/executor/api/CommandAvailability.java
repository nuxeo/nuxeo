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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 *
 */

package org.nuxeo.ecm.platform.commandline.executor.api;

import java.io.Serializable;

/**
 * Represents the availability status of a command. If command is not available, {@link CommandAvailability} contains
 * the errorMessage and some installation instructions.
 *
 * @author tiry
 */
public class CommandAvailability implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final String installMessage;

    protected final String errorMessage;

    protected final boolean available;

    public CommandAvailability(String installMessage, String errorMessage) {
        available = false;
        this.installMessage = installMessage;
        this.errorMessage = errorMessage;
    }

    public CommandAvailability(String errorMessage) {
        this(null, errorMessage);
    }

    public CommandAvailability() {
        available = true;
        installMessage = null;
        errorMessage = null;
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
