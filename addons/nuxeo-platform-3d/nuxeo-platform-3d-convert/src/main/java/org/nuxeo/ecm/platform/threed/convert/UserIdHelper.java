/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Tiago Cardoso <tcardoso@nuxeo.com>
 */
package org.nuxeo.ecm.platform.threed.convert;

import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper class to get java runtime owner user id
 *
 * @since 8.4
 */
public class UserIdHelper {

    public static final String WINDOWS = "Windows";

    public static final String USER_ID_COMMAND = "useruid";

    public static final String USER_PARAM = "username";

    protected static String UID;

    public static String getUid() {
        if (UID == null) {
            if (!WINDOWS.equals(System.getProperty("os.name"))) {
                CmdParameters params = new CmdParameters();
                params.addNamedParameter(USER_PARAM, System.getProperty("user.name"));
                ExecResult result = null;
                try {
                    result = Framework.getService(CommandLineExecutorService.class).execCommand(USER_ID_COMMAND,
                            params);
                    UID = result.getOutput().get(0);
                } catch (CommandNotAvailable commandNotAvailable) {
                    UID = "";
                }
            } else {
                UID = "";
            }
        }
        return UID;
    }
}
