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
package org.nuxeo.ecm.platform.picture.convert.operation;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;

/**
 * @since 5.9.6
 *
 * @author Vincent Vergnolle
 */
@Operation(id = ConverterOperation.ID, category = Constants.CAT_CONVERSION, label = ConverterOperation.ID, description = "Simply call a converter based on the 'converter' parameter. You can pass the converter properties with the 'properties' parameter.", since = "5.9.6")
public class ConverterOperation {

    public static final String ID = "ConverterOperation";

    public static final Log log = LogFactory.getLog(ConverterOperation.class);

    @Param(name = "converter", description = "The name of the converter to call")
    protected String converter;

    @Param(name = "parameters", description = "The converter parameters to pass")
    protected Properties parameters;

    @Context
    protected ConversionService conversionService;

    @OperationMethod
    public Blob run(Blob blob) {
        if (log.isDebugEnabled()) {
            log.debug("Call converter named: " + converter);
        }

        BlobHolder holder = conversionService.convert(converter,
                new SimpleBlobHolder(blob), propertiesToMap());

        return holder.getBlob();
    }

    private Map<String, Serializable> propertiesToMap() {
        if (parameters == null) {
            return Collections.emptyMap();
        }

        Map<String, Serializable> params = new HashMap<>(parameters.size());
        for (Entry<String, String> entry : parameters.entrySet()) {
            Object value = entry.getValue();

            //FIXME: What if it's an integer, a float, a double, etc ...
            Serializable serializable = null;
            try {
                serializable = Integer.valueOf(value.toString());
            } catch (NumberFormatException e) {
                serializable = (Serializable) value;
            }

            params.put(entry.getKey(), serializable);
        }

        return params;
    }
}
