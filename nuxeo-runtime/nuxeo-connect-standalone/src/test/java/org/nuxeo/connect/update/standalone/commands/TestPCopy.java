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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.nuxeo.common.Environment;
import org.nuxeo.connect.update.task.Task;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestPCopy extends AbstractCommandTest {

    @Override
    protected File createPackage() throws IOException, URISyntaxException {
        return getTestPackageZip("test-pcopy");
    }

    @Override
    protected Map<String, String> getUserProperties() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("v", "value");
        return map;
    }

    @Override
    protected void installDone(Task task, Throwable error) throws Exception {
        super.installDone(task, error);
        File dst = getTargetFile();
        assertTrue(dst.isFile());
        assertEquals("test=my value", FileUtils.readFileToString(dst));
    }

    @Override
    protected void uninstallDone(Task task, Throwable error) throws Exception {
        super.uninstallDone(task, error);
        assertFalse(getTargetFile().isFile());
    }

    protected File getTargetFile() {
        return new File(Environment.getDefault().getConfig(), "test.properties");
    }

}
