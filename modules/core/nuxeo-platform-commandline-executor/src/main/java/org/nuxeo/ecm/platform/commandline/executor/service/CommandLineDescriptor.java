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

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * XMap descriptor for a CommandLine.
 *
 * @author tiry
 */
@XObject("command")
@XRegistry(enable = false, compatWarnOnMerge = true)
public class CommandLineDescriptor {

    @XNode("@name")
    @XRegistryId
    protected String name;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled")
    @XEnable
    protected boolean enabled;

    @XNode("commandLine")
    protected String command;

    @XNode(value = "parameterString", defaultAssignment = "")
    protected String parameterString;

    /*
     * @since 8.4
     */
    @XNode(value = "testParameterString", defaultAssignment = "")
    protected String testParameterString;

    @XNode(value = "winParameterString")
    protected String winParameterString;

    /*
     * @since 8.4
     */
    @XNode(value = "winTestParameterString")
    protected String winTestParameterString;

    @XNode("winCommand")
    protected String winCommand;

    @XNode("tester")
    protected String tester;

    @XNode(value = "readOutput", defaultAssignment = "true")
    protected boolean readOutput;

    @XNode("installationDirective")
    protected String installationDirective;

    public String getName() {
        if (name == null) {
            return getCommand();
        }
        return name;
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

    /**
     * @deprecated since 11.5: useless, there is only one executor anyway.
     */
    @Deprecated(since = "11.5")
    public String getExecutor() {
        return CommandLineExecutorComponent.DEFAULT_EXECUTOR;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    // command status management

    /**
     * @deprecated since 11.5: checks done by service and not stored on descriptor anymore.
     */
    @Deprecated(since = "11.5")
    protected boolean available;

    /**
     * @deprecated since 11.5: checks done by service and not stored on descriptor anymore.
     */
    @Deprecated(since = "11.5")
    protected String installErrorMessage;

    /**
     * @deprecated since 11.5: checks done by service and not stored on descriptor anymore.
     */
    @Deprecated(since = "11.5")
    public boolean isAvailable() {
        return available;
    }

    /**
     * @deprecated since 11.5: checks done by service and not stored on descriptor anymore.
     */
    @Deprecated(since = "11.5")
    public void setAvailable(boolean available) {
        this.available = available;
    }

    /**
     * @deprecated since 11.5: checks done by service and not stored on descriptor anymore.
     */
    @Deprecated(since = "11.5")
    public String getInstallErrorMessage() {
        return installErrorMessage;
    }

    /**
     * @deprecated since 11.5: checks done by service and not stored on descriptor anymore.
     */
    @Deprecated(since = "11.5")
    public void setInstallErrorMessage(String installErrorMessage) {
        this.installErrorMessage = installErrorMessage;
    }

}
