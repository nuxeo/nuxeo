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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.automation.core.test;

import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

/**
 *
 *
 * @since 5.9.2
 */
@Operation(id = ParamNameWithAliasOperation.ID)
public class ParamNameWithAliasOperation {

    public static final String ID = "OperationWithParamNameAlias";

    public static final String ALIAS1 = "alias1";

    public static final String ALIAS2 = "alias2";

    @Param(name = "paramName", alias = {ALIAS1, ALIAS2})
    protected String param;

    @OperationMethod
    public String run() {
        return param;
    }

}
