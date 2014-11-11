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
package org.nuxeo.connect.update.impl.task.ant;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.impl.task.AbstractTask;

/**
 * A task based on ant.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class AntTask extends AbstractTask {

    protected AntTask() {
    }

    protected abstract File getAntScript() throws PackageException;

    @SuppressWarnings("unchecked")
    protected void launchAnt(File file, String target,
            Map<String, String> params) throws PackageException {
        AntRunner ant = new AntRunner();
        Map vars = createContextMap(params);
        ant.setGlobalProperties(vars);
        try {
            if (target != null) {
                ant.run(pkg, file, Collections.singletonList(target));
            } else {
                ant.run(pkg, file);
            }
        } catch (Throwable t) {
            throw new PackageException("Install task failed for package "
                    + getPackage().getId(), t);
        }
    }

    protected void doRun(Map<String, String> params) throws PackageException {
        File file = getAntScript();
        if (file.isFile()) {
            launchAnt(file, null, params);
        }
    }

    protected void doRollback() throws PackageException {
        File file = getAntScript();
        if (file.isFile()) {
            launchAnt(file, "rollback", null);
        }
    }

    public void doValidate(ValidationStatus status) {
    }

}
