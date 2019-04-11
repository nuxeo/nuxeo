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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import static org.nuxeo.ecm.platform.threed.convert.Constants.*;
import static org.nuxeo.ecm.platform.threed.convert.Constants.MAX_TEX_PARAMETER;
import static org.nuxeo.ecm.platform.threed.convert.Constants.PERC_TEX_PARAMETER;

/**
 * Level of details converter for 3D document type
 *
 * @since 8.4
 */
public class LodsConverter extends BaseBlenderConverter {

    @Override
    protected Map<String, Blob> getCmdBlobParameters(BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException {
        return null;
    }

    @Override
    protected Map<String, String> getCmdStringParameters(BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException {
        Map<String, String> cmdStringParams = new HashMap<>();

        String percPoly = null;
        if (parameters.containsKey(PERC_POLY_PARAMETER)) {
            percPoly = (String) parameters.get(PERC_POLY_PARAMETER);
        } else if (initParameters.containsKey(PERC_POLY_PARAMETER)) {
            percPoly = initParameters.get(PERC_POLY_PARAMETER);
        }
        cmdStringParams.put(PERC_POLY_PARAMETER, percPoly);

        String maxPoly = null;
        if (parameters.containsKey(MAX_POLY_PARAMETER)) {
            maxPoly = (String) parameters.get(MAX_POLY_PARAMETER);
        } else if (initParameters.containsKey(MAX_POLY_PARAMETER)) {
            maxPoly = initParameters.get(MAX_POLY_PARAMETER);
        }
        cmdStringParams.put(MAX_POLY_PARAMETER, maxPoly);

        String percTex = null;
        if (parameters.containsKey(PERC_TEX_PARAMETER)) {
            percTex = (String) parameters.get(PERC_TEX_PARAMETER);
        } else if (initParameters.containsKey(PERC_TEX_PARAMETER)) {
            percTex = initParameters.get(PERC_TEX_PARAMETER);
        }
        cmdStringParams.put(PERC_TEX_PARAMETER, percTex);

        String maxTex = null;
        if (parameters.containsKey(MAX_TEX_PARAMETER)) {
            maxTex = (String) parameters.get(MAX_TEX_PARAMETER);
        } else if (initParameters.containsKey(MAX_TEX_PARAMETER)) {
            maxTex = initParameters.get(MAX_TEX_PARAMETER);
        }
        cmdStringParams.put(MAX_TEX_PARAMETER, maxTex);

        return cmdStringParams;
    }

}
