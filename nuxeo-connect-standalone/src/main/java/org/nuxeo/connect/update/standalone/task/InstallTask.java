/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.connect.update.standalone.task;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageUpdateService;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class InstallTask extends CommandsTask {

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
        super.doRun(params);
        // generate the uninstall.xml file
        File file = pkg.getData().getEntry(LocalPackage.UNINSTALL);
        writeLog(file);

        // reload components declared in 'reload' file
        reloadComponents(getPackage());
    }

    @Override
    protected void rollbackDone() throws PackageException {
        PackageUpdateService service = Framework.getLocalService(PackageUpdateService.class);
        service.setPackageState(pkg, PackageState.DOWNLOADED);
    }

    @Override
    protected void taskDone() throws PackageException {
        PackageUpdateService service = Framework.getLocalService(PackageUpdateService.class);
        if (isRestartRequired()) {
            service.setPackageState(pkg, PackageState.INSTALLED);
        } else {
            service.setPackageState(pkg, PackageState.STARTED);
        }
    }


    public static void reloadComponents(LocalPackage pkg) throws PackageException {
        File file = pkg.getData().getEntry("reload");
        if (file.isFile()) {
            try {
            List<String> lines = FileUtils.readLines(file);
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("#") || line.length() == 0) {
                    continue;
                }
                reloadComponent(line);
            }
            } catch (IOException e) {
                throw new PackageException("Failed to read the 'reload' file", e);
            }
        }
    }

    public static void reloadComponent(String name) throws PackageException {
        try {
            RegistrationInfoImpl ri = (RegistrationInfoImpl)Framework.getRuntime().getComponentManager().getRegistrationInfo(new ComponentName(name));
            if (ri != null) {
                ri.reload();
            }
        } catch(Exception e) {
            throw new PackageException("Failed to reload component: "+name, e);
        }
    }

}
