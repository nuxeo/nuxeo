/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.connect.update.task.live.commands;

import java.util.Map;

import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.task.standalone.commands.FlushPlaceholder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadService;

/**
 * Flush all Nuxeo caches by calling {@link ReloadService#flush()}
 * <p>
 * The inverse of this command is itself.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Flush extends FlushPlaceholder {

    /**
     * Calls the {@link ReloadService#flush()} method
     *
     * @since 5.6
     * @throws PackageException
     */
    public static void flush() throws PackageException {
        try {
            Framework.getRuntime().reloadProperties();
            ReloadService deployer = Framework.getLocalService(ReloadService.class);
            deployer.flush();
        } catch (Exception e) {
            throw new PackageException("Failed to reload repository", e);
        }
    }

    @Override
    protected Command doRun(Task task, Map<String, String> prefs)
            throws PackageException {
        flush();
        return new Flush();
    }

}
