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
import org.nuxeo.connect.update.task.standalone.commands.FlushCoreCachePlaceholder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadService;

/**
 * Flush any cache held by the core. This should be used when document types
 * are installed or removed.
 * <p>
 * The inverse of this command is itself.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FlushCoreCache extends FlushCoreCachePlaceholder {

    @Override
    protected Command doRun(Task task, Map<String, String> prefs)
            throws PackageException {
        try {
            Framework.getLocalService(ReloadService.class).reloadRepository();
        } catch (Exception e) {
            throw new PackageException("Failed to reload repository", e);
        }
        return new FlushCoreCache();
    }

}
