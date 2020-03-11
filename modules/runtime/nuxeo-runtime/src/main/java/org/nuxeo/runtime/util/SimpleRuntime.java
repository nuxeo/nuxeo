/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.runtime.util;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.runtime.AbstractRuntimeService;
import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.impl.DefaultRuntimeContext;

/**
 * A runtime service used for JUnit tests.
 * <p>
 * The Test Runtime has only one virtual bundle.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class SimpleRuntime extends AbstractRuntimeService {

    private static final Log log = LogFactory.getLog(SimpleRuntime.class);

    public static final String NAME = "Simple Runtime";

    public static final Version VERSION = Version.parseString("1.0.0");

    static int counter = 0;

    public SimpleRuntime() {
        this((File) null);
        try {
            workingDir = Framework.createTempFile("NXTestFramework", generateId());
            workingDir.delete();
        } catch (IOException e) {
            log.error(e, e);
        }
    }

    public SimpleRuntime(String home) {
        this(new File(home));
    }

    public SimpleRuntime(File workingDir) {
        super(new DefaultRuntimeContext());
        this.workingDir = workingDir;
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
        return Long.toHexString(stamp) + '-' + System.identityHashCode(System.class) + '.' + counter;
    }

    @Override
    public void reloadProperties() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
