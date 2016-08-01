/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.commandline.executor.service;

import java.io.Serializable;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.lang3.SystemUtils;

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

    /*
     * @since 8.4
     */
    @XNode("testParameterString")
    protected String testParameterString = "";

    @XNode("winParameterString")
    protected String winParameterString;

    /*
     * @since 8.4
     */
    @XNode("winTestParameterString")
    protected String winTestParameterString = "";

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

    /*
     * @since 8.4
     */
    public String getTestParametersString() {
        if (SystemUtils.IS_OS_WINDOWS && winTestParameterString != null) {
            return winTestParameterString;
        }
        return testParameterString;
    }

    public String getExecutor() {
        return CommandLineExecutorComponent.DEFAULT_EXECUTOR;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
