/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tdelprat
 *
 */
package org.nuxeo.wizard.helpers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.wizard.context.Context;
import org.nuxeo.wizard.context.ParamCollector;

public class ConnectRegistrationHelper {

    public static boolean isConnectRegistrationFileAlreadyPresent(Context ctx) {
        String connectRegistrationFilePath = getConnectRegistrationFile(ctx);
        return new File(connectRegistrationFilePath).exists();
    }

    public static String getConnectRegistrationFile(Context ctx) {
        ParamCollector collector = ctx.getCollector();
        ConfigurationGenerator cg = collector.getConfigurationGenerator();
        String regTargetPath = cg.getDataDir().getAbsolutePath(); // cg.getRuntimeHome();

        if (!regTargetPath.endsWith("/")) {
            regTargetPath = regTargetPath + "/";
        }

        String connectRegistrationFilePath = regTargetPath + "instance.clid";

        return connectRegistrationFilePath;
    }

    public static void saveConnectRegistrationFile(Context ctx)
            throws IOException {

        String connectRegistrationFilePath = getConnectRegistrationFile(ctx);

        String CLID1 = Context.getConnectMap().get("CLID").split("--")[0];
        String CLID2 = Context.getConnectMap().get("CLID").split("--")[1];
        String regFileContent = CLID1 + "\n" + CLID2 + "\nnew instance";

        File regFile = new File(connectRegistrationFilePath);
        FileWriter writer = new FileWriter(regFile);
        writer.write(regFileContent);
        writer.close();
    }

}
