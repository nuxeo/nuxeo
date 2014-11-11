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

package org.nuxeo.ecm.platform.commandline.executor.service;

import java.io.Serializable;

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

    protected boolean avalaible;

    @XNode("commandLine")
    protected String command;

    @XNode("parameterString")
    protected String parameterString;

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

    private static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName.toLowerCase().contains("windows");
    }

    public String getCommand() {
        if (winCommand == null) {
            return command;
        } else {
            if (isWindows()) {
                return winCommand;
            } else {
                return command;
            }
        }
    }

    public String getInstallationDirective() {
        return installationDirective;
    }

    public String getTester() {
        return tester;
    }

    public boolean isAvalaible() {
        return avalaible;
    }

    public void setAvalaible(boolean avalaible) {
        this.avalaible = avalaible;
    }

    public boolean getReadOutput() {
        return readOutput;
    }

    public String getParametersString() {
        if (isWindows() && winParameterString != null) {
            return winParameterString;
        }
        return parameterString;
    }

    public String getExecutor() {
        return CommandLineExecutorComponent.DEFAULT_EXECUTOR;
    }

}
