/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.convert.plugins;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;

public class WordPerfect2TextConverter extends CommandLineBasedConverter {
    @Override
    protected BlobHolder buildResult(List<String> cmdOutput, CmdParameters cmdParams) throws ConversionException {

        StringBuilder sb = new StringBuilder();

        if (cmdOutput != null) {
            for (String out : cmdOutput) {
                if (out != null && out.trim().length() > 0) {
                    sb.append(out.trim());
                    sb.append('\n');
                }
            }
        }
        return new SimpleCachableBlobHolder(Blobs.createBlob(sb.toString()));
    }

    @Override
    protected Map<String, Blob> getCmdBlobParameters(BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException {
        Map<String, Blob> cmdBlobParams = new HashMap<String, Blob>();
        cmdBlobParams.put("inFilePath", blobHolder.getBlob());
        return cmdBlobParams;
    }

    @Override
    protected Map<String, String> getCmdStringParameters(BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException {
        return new HashMap<String, String>();
    }

}
