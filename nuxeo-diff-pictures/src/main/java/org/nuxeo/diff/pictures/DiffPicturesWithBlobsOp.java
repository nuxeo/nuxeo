/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     thibaud
 */

package org.nuxeo.diff.pictures;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;

/**
 * @since 7.4
 */
@Operation(id = DiffPicturesWithBlobsOp.ID, category = Constants.CAT_CONVERSION, label = "Pictures: Diff with Blobs", description = "Compare input blob with blob referenced in the context variable blob2VarName, using the commandLine and its parameters (default values apply). Return the result of the diff as a picture")
public class DiffPicturesWithBlobsOp {

    public static final String ID = "Pictures.DiffWithBlobs";

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @Param(name = "blob2VarName", required = true)
    protected String blob2VarName;

    @Param(name = "commandLine", required = false, values = { "diff-pictures-default" })
    protected String commandLine = "diff-pictures-default";

    @Param(name = "parameters", required = false)
    protected Properties parameters;

    @Param(name = "targetFileName", required = false)
    protected String targetFileName;

    @Param(name = "targetFileNameSuffix", required = false)
    protected String targetFileNameSuffix = "";

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(Blob inBlob) throws OperationException, CommandNotAvailable, IOException {

        Blob blob2 = (Blob) ctx.get(blob2VarName);
        if (blob2 == null) {
            throw new OperationException("The blob to append from variable context: '" + blob2VarName + "' is null.");
        }

        Map<String, Serializable> serializableParameters = new HashMap<>();
        if (parameters != null && parameters.size() > 0) {
            Set<String> parameterNames = parameters.keySet();
            for (String parameterName : parameterNames) {
                serializableParameters.put(parameterName, parameters.get(parameterName));
            }
        }

        if (StringUtils.isNotBlank(targetFileName) || StringUtils.isNotBlank(targetFileNameSuffix)) {
            targetFileName = DiffPicturesUtils.updateTargetFileName(inBlob, targetFileName, targetFileNameSuffix);
            serializableParameters.put("targetFileName", targetFileName);
        }

        DiffPictures dp = new DiffPictures(inBlob, blob2);
        return dp.compare(commandLine, serializableParameters);
    }

}
