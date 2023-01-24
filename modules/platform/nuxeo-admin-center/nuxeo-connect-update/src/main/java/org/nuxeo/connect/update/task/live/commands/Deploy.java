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
package org.nuxeo.connect.update.task.live.commands;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.task.standalone.commands.CompositeCommand;
import org.nuxeo.connect.update.task.standalone.commands.DeployPlaceholder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadContext;
import org.nuxeo.runtime.reload.ReloadResult;
import org.nuxeo.runtime.reload.ReloadService;
import org.osgi.framework.BundleException;

/**
 * Deploy a runtime bundle, or a directory containing runtime bundles.
 * <p>
 * The inverse of this command is Undeploy.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Deploy extends DeployPlaceholder {

    private static final Logger log = LogManager.getLogger(Deploy.class);

    public Deploy() {
        super();
    }

    public Deploy(File file) {
        super(file);
    }

    @Override
    protected Command doRun(Task task, Map<String, String> prefs) throws PackageException {
        if (!file.exists()) {
            log.warn("Can't deploy a non existing file: {}", file);
            return null;
        }
        ReloadService srv = Framework.getService(ReloadService.class);
        if (file.isDirectory()) {
            return deployDirectory(file, srv);
        } else {
            return deployFile(file, srv);
        }
    }

    protected Undeploy deployFile(File file, ReloadService service) throws PackageException {
        String name = service.getOSGIBundleName(file);
        if (name == null) {
            // not an OSGI bundle => ignore
            return null;
        }
        try {
            ReloadResult result = service.reloadBundles(new ReloadContext().deploy(file));
            return result.deployedFilesAsStream()
                         .map(Undeploy::new)
                         .findFirst()
                         .orElseThrow(() -> new IllegalStateException("Bundle " + file + " wasn't deployed"));
        } catch (BundleException e) {
            throw new PackageException("Failed to deploy bundle " + file, e);
        }
    }

    protected CompositeCommand deployDirectory(File dir, ReloadService service) throws PackageException {
        File[] files = dir.listFiles();
        if (files != null) {
            List<File> bundles = Arrays.stream(files)
                                       .filter(f -> service.getOSGIBundleName(f) != null)
                                       .collect(Collectors.toList());
            if (bundles.isEmpty()) {
                // nothing to deploy
                return null;
            }
            try {
                ReloadResult result = service.reloadBundles(new ReloadContext().deploy(bundles));
                return result.deployedFilesAsStream()
                             .map(Undeploy::new)
                             .collect(Collector.of(CompositeCommand::new, CompositeCommand::addCommand,
                                     CompositeCommand::combine));
            } catch (BundleException e) {
                throw new PackageException("Failed to deploy bundles " + bundles, e);
            }
        }
        return null;
    }

}
