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

import java.io.File;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.task.standalone.commands.LoadJarPlaceholder;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated since 5.6
 */
@Deprecated
public class LoadJar extends LoadJarPlaceholder {

    private static final Log log = LogFactory.getLog(LoadJar.class);

    public LoadJar() {
        super();
    }

    public LoadJar(File file) {
        super(file);
    }

    @Override
    protected Command doRun(Task task, Map<String, String> prefs)
            throws PackageException {
        log.warn("LoadJar command is deprecated and does nothing right now");
        return new UnloadJar(file);
    }

}
