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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.jaxrs.io.documents;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.core.operations.business.adapter.BusinessAdapter;
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodecService;
import org.nuxeo.ecm.automation.jaxrs.io.EntityListWriter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.runtime.api.Framework;

/**
 *
 *
 * @since 5.8
 */
public class BusinessAdapterListWriter extends EntityListWriter<BusinessAdapter>{

    @Override
    protected String getEntityType() {
        return "adapters";
    }

    @Override
    protected void writeItem(JsonGenerator jg, BusinessAdapter item)
            throws ClientException, IOException {
        ObjectCodecService service = Framework.getLocalService(ObjectCodecService.class);
        service.write(jg, item);
    }

}
