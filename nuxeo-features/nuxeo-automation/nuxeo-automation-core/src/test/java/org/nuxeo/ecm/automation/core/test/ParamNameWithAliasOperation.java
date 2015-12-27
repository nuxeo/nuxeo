/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.automation.core.test;

import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

/**
 * @since 5.9.2
 */
@Operation(id = ParamNameWithAliasOperation.ID, aliases = { "aliasOp1", "aliasOp2" })
public class ParamNameWithAliasOperation {

    public static final String ID = "OperationWithParamNameAlias";

    public static final String ALIAS_OP = "aliasOp1";

    public static final String ALIAS1 = "alias1";

    public static final String ALIAS2 = "alias2";

    @Param(name = "paramName", alias = { ALIAS1, ALIAS2 })
    protected String param;

    @OperationMethod
    public String run() {
        return param;
    }

}
