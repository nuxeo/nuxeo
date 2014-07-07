/*******************************************************************************
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *******************************************************************************/
package org.nuxeo.ecm.core.storage.sql;

import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.ecm.core.storage.sql.DatabaseH2;
import org.nuxeo.ecm.core.storage.sql.DatabasePostgreSQL;

public class IgnoreNonPooledCondition implements ConditionalIgnoreRule.Condition {

    @Override
    public boolean shouldIgnore() {
        return !(DatabaseHelper.DATABASE instanceof DatabaseH2
                || DatabaseHelper.DATABASE instanceof DatabasePostgreSQL);
    }

}
