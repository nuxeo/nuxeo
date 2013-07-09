/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.model.impl.AbstractRuntimeService;
import org.osgi.framework.Bundle;

/**
 * A runtime service used for JUnit tests.
 * <p>
 * The Test Runtime has only one virtual bundle
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestRuntime extends AbstractRuntimeService {

    public static final String NAME = "Test Runtime";

    public static final Version VERSION = Version.parseString("1.0.0");

    private static int counter = 0;

    public TestRuntime(Bundle runtimeBundle) {
        super(new TestRuntimeContext(runtimeBundle));
        try {
            workingDir = File.createTempFile("NXTestFramework", generateId());
            workingDir.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Version getVersion() {
        return VERSION;
    }

    private static synchronized String generateId() {
        long stamp = System.currentTimeMillis();
        counter++;
        return Long.toHexString(stamp) + '-'
                + System.identityHashCode(System.class) + '.' + counter;
    }

    public void deploy(URL url) throws Exception {
        runtimeContext.deploy(url);
    }

    public void undeploy(URL url) throws Exception {
        runtimeContext.undeploy(url);
    }

    @Override
    public synchronized void stop() throws Exception {
        super.stop();
        if (workingDir != null) {
            FileUtils.deleteTree(workingDir);
        }
    }

    @Override
    public void reloadProperties() throws Exception {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
