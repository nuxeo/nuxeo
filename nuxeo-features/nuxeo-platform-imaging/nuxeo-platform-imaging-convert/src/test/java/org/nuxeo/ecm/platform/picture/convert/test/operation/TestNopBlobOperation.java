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
 *     Vincent Vergnolle
 */
package org.nuxeo.ecm.platform.picture.convert.test.operation;

import javax.inject.Inject;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.picture.convert.operation.NopBlobOperation;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.9.6
 *
 * @author Vincent Vergnolle
 */

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.picture.convert" })
public class TestNopBlobOperation {

    @Inject
    CoreSession session;

    @Inject
    AutomationService automationService;

    @Test
    public void iShouldHaveEqualsBlob() throws Exception {
        Blob blob = new StringBlob("");
        OperationContext context = new OperationContext(session);
        context.setInput(blob);

        Blob resultBlob = (Blob) automationService.run(context,
                NopBlobOperation.ID);

        Assert.assertTrue(blob.equals(resultBlob));
    }
}
