/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.directory.ldap;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.directory.DirectoryCache;

/**
 * @author <a href="mailto:ogrisel@nuxeo.com">Olivier Grisel</a>
 */
public class TestCachedLDAPSession extends TestLDAPSession {

    protected final static String CACHE_CONTRIB = "ldap-directory-cache-config.xml";

    protected final static String ENTRY_CACHE_NAME = "ldap-entry-cache";

    protected final static String ENTRY_CACHE_WITHOUT_REFERENCES_NAME = "ldap-entry-cache-without-references";

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.core.cache");
        deployTestContrib(TEST_BUNDLE, CACHE_CONTRIB);

        List<String> directories = Arrays.asList("userDirectory",
                "groupDirectory");
        for (String directoryName : directories) {
            LDAPDirectory dir = getLDAPDirectory(directoryName);
            DirectoryCache cache = dir.getCache();
            cache.setEntryCacheName(ENTRY_CACHE_NAME);
            cache.setEntryCacheWithoutReferencesName(ENTRY_CACHE_WITHOUT_REFERENCES_NAME);
        }
    }

}
