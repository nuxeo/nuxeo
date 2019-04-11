/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Tiago Cardoso <tcardoso@nuxeo.com>
 */
package org.nuxeo.ecm.platform.threed.convert;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.ExternalConverter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.nuxeo.ecm.platform.threed.convert.Constants.DIMENSIONS_PARAMETER;

/**
 * Render converter 3D document type to PNG
 *
 * @since 8.4
 */
public class RenderConverter extends BaseBlenderConverter implements ExternalConverter {

    @Override
    protected Map<String, Blob> getCmdBlobParameters(BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException {
        return null;
    }

    @Override
    protected Map<String, String> getCmdStringParameters(BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException {
        Map<String, String> cmdStringParams = new HashMap<>();
        String dimensions = null;
        if (parameters.containsKey(DIMENSIONS_PARAMETER)) {
            dimensions = (String) parameters.get(DIMENSIONS_PARAMETER);
        } else if (initParameters.containsKey(DIMENSIONS_PARAMETER)) {
            dimensions = initParameters.get(DIMENSIONS_PARAMETER);
        }
        cmdStringParams.put(DIMENSIONS_PARAMETER, dimensions);
        return cmdStringParams;
    }

}
