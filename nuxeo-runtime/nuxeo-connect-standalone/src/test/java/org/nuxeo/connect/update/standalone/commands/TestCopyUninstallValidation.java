/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.connect.update.standalone.commands;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.commons.io.FileUtils;
import org.nuxeo.connect.update.task.Task;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestCopyUninstallValidation extends TestCopy {

    @Override
    protected void installDone(Task task, Throwable error) throws Exception {
        super.installDone(task, error);
        // modify the target file so that uninstall fails
        FileUtils.writeStringToFile(getTargetFile(), "modified file", UTF_8);
    }

    @Override
    protected void uninstallDone(Task task, Throwable error) throws Exception {
        if (error != null) {
            log.error(error);
            fail("Unexpected Rollback on uninstall Task");
        }
        // since we modified the file the file should be still there (and not
        // deleted by the uninstall)
        assertTrue(getTargetFile().isFile());
        assertEquals("modified file", FileUtils.readFileToString(getTargetFile(), UTF_8));
    }

}
