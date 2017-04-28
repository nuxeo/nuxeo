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

import java.util.Map;

import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.task.standalone.commands.FlushCoreCachePlaceholder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadService;

/**
 * Flush any cache held by the core. This should be used when document types are installed or removed.
 * <p>
 * The inverse of this command is itself.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FlushCoreCache extends FlushCoreCachePlaceholder {

    @Override
    protected Command doRun(Task task, Map<String, String> prefs) throws PackageException {
        try {
            Framework.getService(ReloadService.class).reloadRepository();
        } catch (InterruptedException cause) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while reloading", cause);
        }
        return new FlushCoreCache();
    }

}
