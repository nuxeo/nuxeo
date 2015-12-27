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
import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.task.standalone.commands.CompositeCommand;
import org.nuxeo.connect.update.task.standalone.commands.DeployPlaceholder;
import org.nuxeo.runtime.api.Framework;
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

    private static final Log log = LogFactory.getLog(Deploy.class);

    public Deploy() {
        super();
    }

    public Deploy(File file) {
        super(file);
    }

    protected Undeploy deployFile(File file, ReloadService service) throws PackageException {
        String name = service.getOSGIBundleName(file);
        if (name == null) {
            // not an OSGI bundle => ignore
            return null;
        }
        try {
            service.deployBundle(file, true);
        } catch (BundleException e) {
            throw new PackageException("Failed to deploy bundle " + file, e);
        }
        return new Undeploy(file);
    }

    protected CompositeCommand deployDirectory(File dir, ReloadService service) throws PackageException {
        CompositeCommand cmd = new CompositeCommand();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File fileInDir : files) {
                Command ud = deployFile(fileInDir, service);
                if (ud != null) {
                    cmd.addCommand(ud);
                }
            }
        }
        if (cmd.isEmpty()) {
            // nothing to rollback
            return null;
        }
        return cmd;
    }

    @Override
    protected Command doRun(Task task, Map<String, String> prefs) throws PackageException {
        if (!file.exists()) {
            log.warn("Can't deploy file " + file + ". File is missing.");
            return null;
        }
        ReloadService srv = Framework.getLocalService(ReloadService.class);
        Command rollback;
        if (file.isDirectory()) {
            rollback = deployDirectory(file, srv);
        } else {
            rollback = deployFile(file, srv);
        }
        if (rollback != null) {
            // some deployments where done
            try {
                srv.runDeploymentPreprocessor();
            } catch (IOException e) {
                throw new PackageException(e.getMessage(), e);
            }
        }
        return rollback;
    }

}
