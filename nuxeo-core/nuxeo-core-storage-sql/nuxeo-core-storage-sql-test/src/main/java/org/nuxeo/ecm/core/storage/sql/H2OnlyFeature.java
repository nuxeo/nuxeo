/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.core.storage.sql;

import org.junit.internal.AssumptionViolatedException;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;

/**
 * Allows to ignore all the tests from a class running this feature if the
 * database configured for tests is not H2.
 *
 * @since 5.9.5
 */
public class H2OnlyFeature extends SimpleFeature {

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        if (DatabaseHelper.DATABASE instanceof DatabaseH2) {
            return;
        }
        throw new AssumptionViolatedException("Database is not H2");
    }

}
