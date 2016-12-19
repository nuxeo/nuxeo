/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.connect.update.live;

import java.io.IOException;

import org.nuxeo.common.Environment;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.standalone.StandaloneUpdateService;
import org.nuxeo.connect.update.task.live.LiveInstallTask;
import org.nuxeo.connect.update.task.live.LiveUninstallTask;
import org.nuxeo.connect.update.task.live.commands.Deploy;
import org.nuxeo.connect.update.task.live.commands.DeployConfig;
import org.nuxeo.connect.update.task.live.commands.Flush;
import org.nuxeo.connect.update.task.live.commands.FlushCoreCache;
import org.nuxeo.connect.update.task.live.commands.FlushJaasCache;
import org.nuxeo.connect.update.task.live.commands.ReloadProperties;
import org.nuxeo.connect.update.task.live.commands.RollbackAndUndeploy;
import org.nuxeo.connect.update.task.live.commands.Undeploy;
import org.nuxeo.connect.update.task.live.commands.UndeployConfig;
import org.nuxeo.connect.update.task.live.commands.UpdateAndDeploy;
import org.nuxeo.connect.update.task.update.Rollback;
import org.nuxeo.connect.update.task.update.Update;
import org.nuxeo.runtime.reload.NuxeoRestart;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class UpdateServiceImpl extends StandaloneUpdateService implements PackageUpdateService {

    public UpdateServiceImpl() throws IOException {
        super(Environment.getDefault());
    }

    @Override
    protected void addCommands() {
        super.addCommands();
        addCommand(FlushCoreCache.ID, FlushCoreCache.class);
        addCommand(FlushJaasCache.ID, FlushJaasCache.class);
        addCommand(Flush.ID, Flush.class);

        addCommand(ReloadProperties.ID, ReloadProperties.class);

        addCommand(Deploy.ID, Deploy.class);
        addCommand(Undeploy.ID, Undeploy.class);

        addCommand(DeployConfig.ID, DeployConfig.class);
        addCommand(UndeployConfig.ID, UndeployConfig.class);

        // override the update command to add hot reload support
        addCommand(Update.ID, UpdateAndDeploy.class);
        addCommand(Rollback.ID, RollbackAndUndeploy.class);
    }

    @Override
    public void restart() throws PackageException {
        try {
            NuxeoRestart.restart();
        } catch (IOException e) {
            throw new PackageException("Failed to restart Nuxeo", e);
        }
    }

    @Override
    public String getDefaultInstallTaskType() {
        return LiveInstallTask.class.getName();
    }

    @Override
    public String getDefaultUninstallTaskType() {
        return LiveUninstallTask.class.getName();
    }

}
