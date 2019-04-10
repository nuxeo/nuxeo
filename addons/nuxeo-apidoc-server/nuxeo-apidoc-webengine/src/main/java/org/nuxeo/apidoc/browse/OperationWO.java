/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.apidoc.browse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationDocumentation.Param;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.jaxrs.io.JsonWriter;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.runtime.api.Framework;

@WebObject(type = "operation")
public class OperationWO extends NuxeoArtifactWebObject {

    @Override
    @GET
    @Produces("text/html")
    @Path("introspection")
    public Object doGet() {
        return getView("view").arg("operation", getTargetComponentInfo());
    }

    public OperationInfo getTargetComponentInfo() {
        return getSnapshotManager().getSnapshot(getDistributionId(), ctx.getCoreSession()).getOperation(nxArtifactId);
    }

    @Override
    public NuxeoArtifact getNxArtifact() {
        return getTargetComponentInfo();
    }

    protected String[] getInputs(OperationInfo op) {
        String[] signature = op.getSignature();
        if (signature == null || signature.length == 0) {
            return new String[0];
        }
        String[] result = new String[signature.length / 2];
        for (int i = 0, k = 0; i < signature.length; i += 2, k++) {
            result[k] = signature[i];
        }
        return result;
    }

    protected String[] getOutputs(OperationInfo op) {
        String[] signature = op.getSignature();
        if (signature == null || signature.length == 0) {
            return new String[0];
        }
        String[] result = new String[signature.length / 2];
        for (int i = 1, k = 0; i < signature.length; i += 2, k++) {
            result[k] = signature[i];
        }
        return result;
    }

    public String getInputsAsString(OperationInfo op) {
        String[] result = getInputs(op);
        if (result == null || result.length == 0) {
            return "void";
        }
        return StringUtils.join(result, ", ");
    }

    public String getOutputsAsString(OperationInfo op) {
        String[] result = getOutputs(op);
        if (result == null || result.length == 0) {
            return "void";
        }
        return StringUtils.join(result, ", ");
    }

    public String getParamDefaultValue(Param param) {
        if (param.values != null && param.values.length > 0) {
            return StringUtils.join(param.values, ", ");
        }
        return "";
    }

    @GET
    @Produces("text/html")
    @Override
    public Object doViewDefault() {
        Template t = (Template) super.doViewDefault();
        try {
            OperationDocumentation opeDoc = Framework.getService(AutomationService.class)
                                                     .getOperation(nxArtifactId)
                                                     .getDocumentation();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JsonWriter.writeOperation(baos, opeDoc, true);
            t.arg("json", baos.toString());
        } catch (OperationException | IOException e) {
            throw WebException.wrap(e);
        }
        return t;
    }

    @Override
    public String getSearchCriterion() {
        return "'" + super.getSearchCriterion() + "' Operation";
    }
}
