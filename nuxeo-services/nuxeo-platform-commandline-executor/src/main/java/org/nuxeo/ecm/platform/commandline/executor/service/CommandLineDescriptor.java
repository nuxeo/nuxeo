/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.commandline.executor.service;

import java.io.Serializable;

import org.apache.commons.lang.SystemUtils;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * XMap descriptor for a CommandLine.
 *
 * @author tiry
 */
@XObject("command")
public class CommandLineDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected boolean enabled;

    protected boolean available;

    @XNode("commandLine")
    protected String command;

    @XNode("parameterString")
    protected String parameterString = "";

    @XNode("winParameterString")
    protected String winParameterString;

    @XNode("winCommand")
    protected String winCommand;

    @XNode("tester")
    protected String tester;

    @XNode("readOutput")
    protected boolean readOutput = true;

    @XNode("installationDirective")
    protected String installationDirective;

    protected String installErrorMessage;

    public String getInstallErrorMessage() {
        return installErrorMessage;
    }

    public void setInstallErrorMessage(String installErrorMessage) {
        this.installErrorMessage = installErrorMessage;
    }

    public String getName() {
        if (name == null) {
            return getCommand();
        }
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getCommand() {
        if (SystemUtils.IS_OS_WINDOWS && winCommand != null) {
            return winCommand;
        }
        return command;
    }

    public String getInstallationDirective() {
        return installationDirective;
    }

    public String getTester() {
        return tester;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean getReadOutput() {
        return readOutput;
    }

    public String getParametersString() {
        if (SystemUtils.IS_OS_WINDOWS && winParameterString != null) {
            return winParameterString;
        }
        return parameterString;
    }

    public String getExecutor() {
        return CommandLineExecutorComponent.DEFAULT_EXECUTOR;
    }

}
