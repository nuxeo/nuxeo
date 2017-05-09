/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.connect.update.task.live;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.task.live.commands.Flush;
import org.nuxeo.connect.update.task.standalone.InstallTask;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.impl.RegistrationInfoImpl;
import org.nuxeo.runtime.reload.ReloadService;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class LiveInstallTask extends InstallTask {

    public LiveInstallTask(PackageUpdateService pus) {
        super(pus);
    }

    @Override
    protected void doRun(Map<String, String> params) throws PackageException {
        super.doRun(params);
        // reload components declared in 'reload' file
        reloadComponents(getPackage());
    }

    @Override
    protected void taskDone() throws PackageException {
        try {
            Framework.getService(ReloadService.class).reload();
        } catch (InterruptedException cause) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while reloading runtime", cause);
        }
        if (isRestartRequired()) {
            service.setPackageState(pkg, PackageState.INSTALLED);
        } else {
            service.setPackageState(pkg, PackageState.STARTED);
        }
    }

    /**
     * @deprecated since 5.6: this way of reloading components is smarter because the package installed can declare what
     *             needs to be reloaded exactly, but this is too complicated to handle, and risky given potential
     *             dependency issues => make components listen for the "flush" event instead, @see {@link ReloadService}
     */
    @Deprecated
    protected static void reloadComponents(LocalPackage localPackage) throws PackageException {
        File file = localPackage.getData().getEntry("reload");
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

    /**
     * @deprecated since 5.6: see {@link #reloadComponents(LocalPackage)}
     */
    @Deprecated
    protected static void reloadComponent(String name) throws PackageException {
        RegistrationInfoImpl ri = (RegistrationInfoImpl) Framework.getRuntime().getComponentManager().getRegistrationInfo(
                new ComponentName(name));
        if (ri != null) {
            ri.reload();
        }
    }

    @Override
    protected void flush() throws PackageException {
        Flush.flush();
    }

}
