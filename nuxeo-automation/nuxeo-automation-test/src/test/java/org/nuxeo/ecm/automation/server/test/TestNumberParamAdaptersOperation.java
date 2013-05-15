/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.automation.server.test;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.impl.adapters.IntegerToLong;

/**
 * Operation to test number parameter adapters, such as {@link IntegerToLong}.
 *
 * @author Antoine Taillefer
 */
@Operation(id = TestNumberParamAdaptersOperation.ID, category = Constants.CAT_EXECUTION, label = "Test number parameter adapters, such as IntegerToLong.")
public class TestNumberParamAdaptersOperation {

    public static final String ID = "TestNumberParamAdaptersOperation";

    @Param(name = "longParam")
    protected long longParam;

    @OperationMethod
    public Long longParam() {
        return longParam;
    }

}
