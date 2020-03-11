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

import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.update.Rollback;
import org.nuxeo.connect.update.task.update.RollbackOptions;
import org.nuxeo.connect.update.task.update.Update;
import org.nuxeo.connect.update.task.update.UpdateManager;

/**
 * Live version of the update command, that handle hot-reloading of the jar by deploying it to the runtime.
 *
 * @since 5.6
 */
public class UpdateAndDeploy extends Update {

    // needed for deserialization
    public UpdateAndDeploy() {
        super();
    }

    @Override
    protected Command getDeployCommand(UpdateManager updateManager, Command rollbackCommand) {
        // file is the file to be deployed, so it's not in its final place.
        // But deploy should use the final place => extract info from the
        // rollback command...
        if (rollbackCommand instanceof Rollback) {
            // FIXME: only handle one file right now => deploy the file
            // with options given by rollback, only when it's not a composite
            // command
            Rollback rollback = (Rollback) rollbackCommand;
            RollbackOptions opt = rollback.getRollbackOptions();
            File rollbackTarget = updateManager.getRollbackTarget(opt);
            if (rollbackTarget != null) {
                return new Deploy(rollbackTarget);
            } else {
                return null;
            }
        }
        return new Deploy(file);
    }

}
