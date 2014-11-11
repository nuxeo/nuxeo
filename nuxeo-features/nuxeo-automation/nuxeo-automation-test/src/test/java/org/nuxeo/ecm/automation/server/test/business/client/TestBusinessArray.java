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
 */
package org.nuxeo.ecm.automation.server.test.business.client;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentException;

@Operation(id = TestBusinessArray.ID, category = Constants.CAT_SERVICES,
        label = "", description = "")
public class TestBusinessArray {

    public static final String ID = "Business.TestBusinessArray";

    @OperationMethod
    public BusinessBean[] run() throws ClientException,
            DocumentException {
        BusinessBean[] businessBeans = new BusinessBean[] { new BusinessBean
                (), new BusinessBean() };
        return businessBeans;
    }

}
