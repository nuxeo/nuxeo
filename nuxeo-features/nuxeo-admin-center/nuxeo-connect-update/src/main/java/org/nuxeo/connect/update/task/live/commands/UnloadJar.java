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

import java.io.File;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.task.standalone.commands.UnloadJarPlaceholder;

/**
 * Does nothing right now.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated since 5.6
 */
@Deprecated
public class UnloadJar extends UnloadJarPlaceholder {

    private static final Log log = LogFactory.getLog(UnloadJar.class);

    public UnloadJar() {
        super();
    }

    public UnloadJar(File file) {
        super(file);
    }

    @Override
    protected Command doRun(Task task, Map<String, String> prefs) throws PackageException {
        log.warn("UnloadJar command is deprecated and does nothing right now");
        return new LoadJar(file);
    }

}
