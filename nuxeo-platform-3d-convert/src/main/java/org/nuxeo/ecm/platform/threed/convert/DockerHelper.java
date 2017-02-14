/*
 * (C) Copyright 2006-2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 * @since 8.10
 */
public class DockerHelper {

    public static final String CREATE_CONTAINER_COMMAND = "create_container";
    public static final String REMOVE_CONTAINER_COMMAND = "remove_container";
    public static final String COPY_CONTAINER_COMMAND = "copy_container";

    public static final String NAME_PARAM = "name";
    public static final String IMAGE_PARAM = "image";
    public static final String SOURCE_PARAM = "source";
    public static final String DEST_PARAM = "destination";

    public static boolean CreateContainer(String name, String image) {
        CmdParameters params = new CmdParameters();
        params.addNamedParameter(NAME_PARAM, name);
        params.addNamedParameter(IMAGE_PARAM, image);
        ExecResult result = null;
        try {
            result = Framework.getService(CommandLineExecutorService.class).execCommand(CREATE_CONTAINER_COMMAND, params);
        } catch (CommandNotAvailable commandNotAvailable) {
            return false;
        }
        return true;
    }

    public static boolean RemoveContainer(String name) {
        CmdParameters params = new CmdParameters();
        params.addNamedParameter(NAME_PARAM, name);
        ExecResult result = null;
        try {
            result =  Framework.getService(CommandLineExecutorService.class).execCommand(REMOVE_CONTAINER_COMMAND, params);
        } catch (CommandNotAvailable commandNotAvailable) {
            return false;
        }
        return true;
    }

    public static boolean CopyData( String source, String destination) {
        CmdParameters params = new CmdParameters();
        params.addNamedParameter(SOURCE_PARAM, source);
        params.addNamedParameter(DEST_PARAM, destination);
        ExecResult result = null;
        try {
            result = Framework.getService(CommandLineExecutorService.class).execCommand(COPY_CONTAINER_COMMAND, params);
        } catch (CommandNotAvailable commandNotAvailable) {
            return false;
        }
        return true;
    }

}
