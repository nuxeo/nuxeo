/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.apidoc.browse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.ecm.automation.OperationDocumentation.Param;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.webengine.model.WebObject;

@WebObject(type = "operation")
public class OperationWO extends NuxeoArtifactWebObject {

    @Override
    @GET
    @Produces("text/html")
    @Path("introspection")
    public Object doGet() throws Exception {
        return getView("view").arg("operation", getTargetComponentInfo());
    }

    public OperationInfo getTargetComponentInfo() throws OperationException {
        return getSnapshotManager().getSnapshot(getDistributionId(),
                ctx.getCoreSession()).getOperation(nxArtifactId);
    }

    @Override
    public NuxeoArtifact getNxArtifact() throws OperationException {
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

}
