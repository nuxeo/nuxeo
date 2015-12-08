/*
 * (C) Copyright 2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     ron1
 */
package org.nuxeo.ecm.platform.rendition.service;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

@Operation(id = SleepOperation.ID, category = Constants.CAT_EXECUTION, label = "Sleep", description = "Sleep for durationMillis.")
public class SleepOperation {

    public static final String ID = "SleepOperation";

    @Param(name = "durationMillis", required = false)
    protected long durationMillis = 0;

    @OperationMethod
    public void run() {
        try {
            Thread.sleep(durationMillis);
        } catch (InterruptedException e) {
            // restore interrupted status
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

}
