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

import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.update.Update;

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
    protected Command getDeployCommand() {
        return new Deploy(file);
    }

}
