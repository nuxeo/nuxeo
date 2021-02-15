/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.directory.multi;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.memory.MemoryDirectory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.NuxeoLoginContext;

/**
 * Helper methods factored out for testing.
 *
 * @since 11.5
 */
public class TestDirectoryHelper {

    private TestDirectoryHelper() {
    }

    public static void fillMemDir(MemoryDirectory dir, List<Map<String, Object>> entries) {
        assertNotNull(dir);
        try (Session session = dir.getSession()) {
            entries.forEach(session::createEntry);
        }
    }

    @SuppressWarnings("deprecation")
    public static void clearDirectory(MemoryDirectory dir) {
        if (dir != null) {
            // as admin
            try {
                NuxeoLoginContext loginContext = Framework.loginUser(SecurityConstants.ADMINISTRATOR);
                dir.setReadOnly(false);
                try (Session session = dir.getSession()) {
                    session.getEntries().forEach(session::deleteEntry);
                } finally {
                    loginContext.close();
                    loginContext = null;
                }
            } catch (LoginException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
