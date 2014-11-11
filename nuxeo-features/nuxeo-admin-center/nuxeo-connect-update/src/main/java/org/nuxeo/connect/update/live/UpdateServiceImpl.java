/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import org.nuxeo.connect.update.task.live.commands.Install;
import org.nuxeo.connect.update.task.live.commands.LoadJar;
import org.nuxeo.connect.update.task.live.commands.ReloadProperties;
import org.nuxeo.connect.update.task.live.commands.RollbackAndUndeploy;
import org.nuxeo.connect.update.task.live.commands.Undeploy;
import org.nuxeo.connect.update.task.live.commands.UndeployConfig;
import org.nuxeo.connect.update.task.live.commands.Uninstall;
import org.nuxeo.connect.update.task.live.commands.UnloadJar;
import org.nuxeo.connect.update.task.live.commands.UpdateAndDeploy;
import org.nuxeo.connect.update.task.update.Rollback;
import org.nuxeo.connect.update.task.update.Update;
import org.nuxeo.runtime.reload.NuxeoRestart;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class UpdateServiceImpl extends StandaloneUpdateService implements
        PackageUpdateService {

    public UpdateServiceImpl() throws IOException {
        super(Environment.getDefault());
    }

    @Override
    protected void addCommands() {
        super.addCommands();
        addCommand(Install.ID, Install.class);
        addCommand(Uninstall.ID, Uninstall.class);
        addCommand(FlushCoreCache.ID, FlushCoreCache.class);
        addCommand(FlushJaasCache.ID, FlushJaasCache.class);
        addCommand(Flush.ID, Flush.class);

        addCommand(ReloadProperties.ID, ReloadProperties.class);

        addCommand(Deploy.ID, Deploy.class);
        addCommand(Undeploy.ID, Undeploy.class);

        addCommand(DeployConfig.ID, DeployConfig.class);
        addCommand(UndeployConfig.ID, UndeployConfig.class);

        addCommand(LoadJar.ID, LoadJar.class);
        addCommand(UnloadJar.ID, UnloadJar.class);

        // override the update command to add hot reload support
        addCommand(Update.ID, UpdateAndDeploy.class);
        addCommand(Rollback.ID, RollbackAndUndeploy.class);
    }

    @Override
    public void restart() throws PackageException {
        try {
            NuxeoRestart.restart();
        } catch (Throwable t) {
            throw new PackageException("Failed to restart Nuxeo", t);
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
