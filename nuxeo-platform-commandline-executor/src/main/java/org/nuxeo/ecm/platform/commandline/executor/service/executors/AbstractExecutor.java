/*
 * (C) Copyright 2006-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tdelprat, jcarsique
 *
 */

package org.nuxeo.ecm.platform.commandline.executor.service.executors;

import java.util.ArrayList;
import java.util.List;
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

    /**
     * Returns parameters as a String after having replaced parameterized
     * values inside.
     *
     * @param cmdDesc CommandLineDescriptor containing parameters
     * @param params parameterized values
     * @return Parameters as a String
     */
    public static String getParametersString(CommandLineDescriptor cmdDesc,
            CmdParameters params) {
        String paramString = cmdDesc.getParametersString();
        Map<String, String> paramsValues = params.getParameters();
        paramString = replaceParams(paramsValues, paramString);
        return paramString;
    }

    /**
     * Returns parameters as a String array after having replaced parameterized
     * values inside.
     *
     * @param cmdDesc CommandLineDescriptor containing parameters
     * @param params parameterized values
     * @return Parameters as a String array
     * @since 5.5
     */
    public static String[] getParametersArray(CommandLineDescriptor cmdDesc,
            CmdParameters params) {
        List<String> res = new ArrayList<String>();
        String[] paramsArray = cmdDesc.getParametersString().split(" ");
        Map<String, String> paramsValues = params.getParameters();
        for (String paramString : paramsArray) {
            res.add(replaceParams(paramsValues, paramString));
        }
        return res.toArray(new String[] {});
    }

    private static String replaceParams(Map<String, String> paramsValues,
            String paramString) {
        for (String pname : paramsValues.keySet()) {
            paramString = paramString.replace("#{" + pname + "}",
                    paramsValues.get(pname));
        }
        return paramString;
    }

}
