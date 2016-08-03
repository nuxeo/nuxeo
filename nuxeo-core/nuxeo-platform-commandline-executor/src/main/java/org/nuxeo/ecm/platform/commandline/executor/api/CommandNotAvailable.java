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

    /**
     * @since 5.5
     */
    @Override
    public String getMessage() {
        String msg = getErrorMessage() != null ? getErrorMessage() + ". " : "";
        msg += getInstallMessage() != null ? getInstallMessage() + ". " : "";
        return msg + super.getMessage();
    }

}
