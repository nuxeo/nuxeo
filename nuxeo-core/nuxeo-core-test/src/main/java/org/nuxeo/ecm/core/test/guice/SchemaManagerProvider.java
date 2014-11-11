/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.core.test.guice;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class SchemaManagerProvider implements Provider<SchemaManager> {

    private static final Log log = LogFactory.getLog(SchemaManagerProvider.class);

    private final RuntimeHarness harness;

    @Inject
    public SchemaManagerProvider(RuntimeHarness harness) {
        this.harness = harness;
    }

    public SchemaManager get() {
        try {
            harness.deployBundle("org.nuxeo.ecm.core.schema");
            return Framework.getService(SchemaManager.class);
        } catch (Exception e) {
            log.error(e.toString(), e);
            return null;
        }
    }

}
