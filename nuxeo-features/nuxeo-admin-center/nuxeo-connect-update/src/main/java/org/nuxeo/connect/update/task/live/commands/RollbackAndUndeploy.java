/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.connect.update.task.live.commands;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.task.update.Rollback;
import org.nuxeo.connect.update.task.update.RollbackOptions;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadService;

/**
 * @since 5.6
 */
public class RollbackAndUndeploy extends Rollback {

    // needed for deserialization
    public RollbackAndUndeploy() {
        super();
    }

    public RollbackAndUndeploy(RollbackOptions opt) {
        super(opt);
    }

    @Override
    protected Command doRun(Task task, Map<String, String> prefs) throws PackageException {
        Command res = null;
        try {
            res = super.doRun(task, prefs);

            // then re-build the war now that jar is deleted
            ReloadService srv = Framework.getService(ReloadService.class);
            srv.runDeploymentPreprocessor();
        } catch (PackageException | IOException e) {
            // ignore uninstall -> this may break the entire chain. Usually
            // uninstall is done only when rollbacking or uninstalling => force
            // restart required
            task.setRestartRequired(true);
            throw new PackageException("Failed to undeploy bundle", e);
        }
        return res;
    }

    protected Command getUndeployCommand(File targetFile) {
        return new Undeploy(targetFile);
    }

}
