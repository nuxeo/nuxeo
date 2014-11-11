/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * Live version of the update command, that handle hot-reloading of the jar by
 * deploying it to the runtime.
 *
 * @since 5.6
 */
public class UpdateAndDeploy extends Update {

    // needed for deserialization
    public UpdateAndDeploy() {
        super();
    }

    @Override
    protected Command getDeployCommand(UpdateManager updateManager,
            Command rollbackCommand) {
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
