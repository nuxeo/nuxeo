/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.automation.server.test.operations;

import java.util.List;
import java.util.stream.Collectors;

import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * @since 10.10
 */
@Operation(id = UpdateMailOperation.ID)
public class UpdateMailOperation {

    @Param(name = "users")
    protected StringList users;

    @Context
    protected UserManager userManager;

    public static final String ID = "updateMail";

    public static final String TEST_EMAIL = "test@nuxeo.com";

    @OperationMethod
    public List<NuxeoPrincipal> run() {
        return users.stream()
                    .map(username -> userManager.getPrincipal(username))
                    .peek(user -> user.setEmail(TEST_EMAIL))
                    .collect(Collectors.toList());
    }

}
