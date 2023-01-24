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

import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.task.live.commands.Flush;
import org.nuxeo.connect.update.task.standalone.InstallTask;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadService;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class LiveInstallTask extends InstallTask {

    public LiveInstallTask(PackageUpdateService pus) {
        super(pus);
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

    @Override
    protected void flush() throws PackageException {
        Flush.flush();
    }

}
