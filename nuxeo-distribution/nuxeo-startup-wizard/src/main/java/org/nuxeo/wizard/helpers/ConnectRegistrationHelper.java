/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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

    public static void saveConnectRegistrationFile(Context ctx) throws IOException {

        String connectRegistrationFilePath = getConnectRegistrationFile(ctx);

        String CLID1 = Context.getConnectMap().get("CLID").split("--")[0];
        String CLID2 = Context.getConnectMap().get("CLID").split("--")[1];
        String regFileContent = CLID1 + "\n" + CLID2 + "\nnew instance";

        File regFile = new File(connectRegistrationFilePath);
        try (FileWriter writer = new FileWriter(regFile)) {
            writer.write(regFileContent);
        }
    }

}
