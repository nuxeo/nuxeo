/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.connect.update.task.live.commands;

import java.io.File;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.task.standalone.commands.UndeployPlaceholder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadService;
import org.osgi.framework.BundleException;

/**
 * Undeploy a runtime bundle, or a directory containing runtime bundles.
 * <p>
 * The inverse of this command is Deploy.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Undeploy extends UndeployPlaceholder {

    private static final Log log = LogFactory.getLog(Undeploy.class);

    public Undeploy() {
        super();
    }

    public Undeploy(File file) {
        super(file);
    }

    protected void undeployFile(File file, ReloadService service) throws PackageException {
        String name = service.getOSGIBundleName(file);
        if (name == null) {
            // not an OSGI bundle => ignore
            return;
        }
        try {
            service.undeployBundle(file, true);
        } catch (BundleException e) {
            throw new PackageException("Failed to undeploy bundle " + file, e);
        }
    }

    protected void undeployDirectory(File dir, ReloadService service) throws PackageException {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File fileInDir : files) {
                undeployFile(fileInDir, service);
            }
        }
    }

    @Override
    protected Command doRun(Task task, Map<String, String> prefs) throws PackageException {
        if (!file.exists()) {
            log.warn("Can't undeploy file " + file + ". File is missing.");
            return null;
        }
        try {
            ReloadService srv = Framework.getLocalService(ReloadService.class);
            if (file.isDirectory()) {
                undeployDirectory(file, srv);
            } else {
                undeployFile(file, srv);
            }
        } catch (PackageException e) {
            // ignore uninstall -> this may break the entire chain. Usually
            // uninstall is done only when rollbacking or uninstalling => force
            // restart required
            task.setRestartRequired(true);
            throw new PackageException("Failed to undeploy bundle " + file, e);
        }
        return new Deploy(file);
    }

}
