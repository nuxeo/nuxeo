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
package org.nuxeo.connect.update.standalone.commands;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.task.Task;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestCopyInstallValidation extends TestCopy {

    @Test
    @Override
    public void testInstallThenUninstall() throws Exception {
        // create the target file so that the copy command will not validate
        getTargetFile().createNewFile();
        super.testInstallThenUninstall();
    }

    @Override
    protected boolean validateInstall(Task task, ValidationStatus status) {
        if (!status.hasErrors()) {
            fail("Expected copy command to be invalid");
        }
        return false;
    }

}
