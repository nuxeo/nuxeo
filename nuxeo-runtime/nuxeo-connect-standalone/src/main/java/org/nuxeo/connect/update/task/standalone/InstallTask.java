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
 *     bstefanescu, jcarsique
 */
package org.nuxeo.connect.update.task.standalone;

import java.io.File;
import java.util.Map;

import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageUpdateService;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class InstallTask extends CommandsTask {

    public InstallTask(PackageUpdateService pus) {
        super(pus);
    }

    @Override
    public boolean isInstallTask() {
        return true;
    }

    @Override
    protected File getCommandsFile() throws PackageException {
        return pkg.getInstallFile();
    }

    @Override
    protected void doRun(Map<String, String> params) throws PackageException {
        try {
            super.doRun(params);
        } finally {
            // generate the uninstall.xml file
            File file = pkg.getData().getEntry(LocalPackage.UNINSTALL);
            writeLog(file);
            // No reload of components in standalone mode
        }
    }

    @Override
    protected void rollbackDone() throws PackageException {
        service.setPackageState(pkg, PackageState.DOWNLOADED);
    }

    @Override
    protected void taskDone() throws PackageException {
        service.setPackageState(pkg, PackageState.STARTED);
    }

    @Override
    protected void flush() throws PackageException {
        // standalone mode: nothing to do
    }

}
