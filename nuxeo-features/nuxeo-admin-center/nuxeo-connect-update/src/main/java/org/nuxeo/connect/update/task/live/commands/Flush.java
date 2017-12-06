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

import java.io.IOException;
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
            ReloadService deployer = Framework.getService(ReloadService.class);
            deployer.flush();
        } catch (IOException e) {
            throw new PackageException("Failed to reload repository", e);
        }
    }

    @Override
    protected Command doRun(Task task, Map<String, String> prefs) throws PackageException {
        flush();
        return new Flush();
    }

}
