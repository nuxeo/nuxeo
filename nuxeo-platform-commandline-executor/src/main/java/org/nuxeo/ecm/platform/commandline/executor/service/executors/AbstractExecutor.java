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

package org.nuxeo.ecm.platform.commandline.executor.service.executors;

import java.util.Map;

import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.service.CommandLineDescriptor;

/**
 * Base class for {@link Executor}.
 *
 * @author tiry
 */
public abstract class AbstractExecutor {

    public static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName.toLowerCase().contains("windows");
    }

    public static String getParametersString(CommandLineDescriptor cmdDesc, CmdParameters params) {
        String paramString = cmdDesc.getParametersString();
        Map<String, String> paramsValues = params.getParameters();

        for (String pname : paramsValues.keySet()) {
            paramString = paramString.replace("#{" + pname + "}", paramsValues.get(pname));
        }
        return paramString;
    }

}
