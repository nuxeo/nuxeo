/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.server.jaxrs.doc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.trace.Trace;
import org.nuxeo.ecm.automation.core.trace.TracerFactory;
import org.nuxeo.ecm.automation.io.yaml.YamlWriter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebObject(type = "doc")
@Produces("text/html;charset=UTF-8")
public class DocResource extends AbstractResource<ResourceTypeImpl> {

    private final Log log = LogFactory.getLog(DocResource.class);

    protected AutomationService service;

    protected List<OperationDocumentation> ops;

    @Override
    public void initialize(Object... args) {
        try {
            service = Framework.getLocalService(AutomationService.class);
            ops = service.getDocumentation();
        } catch (OperationException e) {
            log.error("Failed to get automation service", e);
            throw WebException.wrap(e);
        }
    }

    protected Template getTemplateFor(String browse) {
        return getTemplateView("index").arg("browse", browse);
    }

    protected Template getTemplateView(String name) {
        Map<String, List<OperationDocumentation>> cats = new HashMap<String, List<OperationDocumentation>>();
        for (OperationDocumentation op : ops) {
            List<OperationDocumentation> list = cats.get(op.getCategory());
            if (list == null) {
                list = new ArrayList<OperationDocumentation>();
                cats.put(op.getCategory(), list);
            }
            list.add(op);
        }
        // sort categories
        List<String> catNames = new ArrayList<>();
        catNames.addAll(cats.keySet());
        Collections.sort(catNames);
        Map<String, List<OperationDocumentation>> scats = new LinkedHashMap<String, List<OperationDocumentation>>();
        for (String catName : catNames) {
            scats.put(catName, cats.get(catName));
        }
        return getView(name).arg("categories", scats).arg("operations", ops);
    }

    @GET
    public Object doGet(@QueryParam("id") String id, @QueryParam("browse") String browse) {
        if (id == null) {
            return getTemplateFor(browse);
        } else {
            OperationDocumentation opDoc = null;
            for (OperationDocumentation op : ops) {
                if (op.getId().equals(id)) {
                    opDoc = op;
                    break;
                }
            }
            if (opDoc == null) {
                throw new WebApplicationException(Response.status(404).build());
            }
            Template tpl = getTemplateFor(browse);
            tpl.arg("operation", opDoc);
            CoreSession session = SessionFactory.getSession();
            if (((NuxeoPrincipal) session.getPrincipal()).isAdministrator()) {
                // add yaml format - chains are exposing their operations
                // so this information should be restricted to administrators.
                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    YamlWriter.toYaml(out, opDoc);
                    tpl.arg("yaml", out.toString());
                } catch (IOException e) {
                    throw WebException.wrap(e);
                }
            }
            return tpl;
        }
    }

    protected boolean canManageTraces() {
        return ((NuxeoPrincipal) WebEngine.getActiveContext().getPrincipal()).isAdministrator();
    }

    @GET
    @Path("/wiki")
    public Object doGetWiki() {
        return getTemplateView("wiki");
    }

    public boolean isTraceEnabled() {
        TracerFactory tracerFactory = Framework.getLocalService(TracerFactory.class);
        return tracerFactory.getRecordingState();
    }

    @GET
    @Path("/toggleTraces")
    public Object toggleTraces() {
        if (!canManageTraces()) {
            return "You can not manage traces";
        }
        TracerFactory tracerFactory = Framework.getLocalService(TracerFactory.class);
        tracerFactory.toggleRecording();
        HttpServletRequest request = RequestContext.getActiveContext().getRequest();
        String url = request.getHeader("Referer");
        try {
            return Response.seeOther(new URI(url)).build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/toggleStackDisplay")
    @Produces("text/plain")
    public Object toggleStackDisplay() {
        if (!canManageTraces()) {
            return "You can not manage json exception stack display";
        }
        JsonFactoryManager jsonFactoryManager = Framework.getLocalService(JsonFactoryManager.class);
        return String.valueOf(jsonFactoryManager.toggleStackDisplay());
    }

    @GET
    @Path("/traces")
    @Produces("text/plain")
    public String doGetTrace(@QueryParam("opId") String opId) {
        if (!canManageTraces()) {
            return "You can not manage traces";
        }
        TracerFactory tracerFactory = Framework.getLocalService(TracerFactory.class);
        Trace trace = tracerFactory.getTrace(opId);
        if (trace == null) {
            return "no trace";
        }
        return tracerFactory.print(trace);
    }

    public String[] getInputs(OperationDocumentation op) {
        if (op == null) {
            throw new IllegalArgumentException("Operation must not be null");
        }
        if (op.signature == null || op.signature.length == 0) {
            return new String[0];
        }
        String[] result = new String[op.signature.length / 2];
        for (int i = 0, k = 0; i < op.signature.length; i += 2, k++) {
            result[k] = op.signature[i];
        }
        return result;
    }

    public String[] getOutputs(OperationDocumentation op) {
        if (op == null) {
            throw new IllegalArgumentException("Operation must not be null");
        }
        if (op.signature == null || op.signature.length == 0) {
            return new String[0];
        }
        String[] result = new String[op.signature.length / 2];
        for (int i = 1, k = 0; i < op.signature.length; i += 2, k++) {
            result[k] = op.signature[i];
        }
        return result;
    }

    public String getInputsAsString(OperationDocumentation op) {
        String[] result = getInputs(op);
        if (result == null || result.length == 0) {
            return "void";
        }
        return StringUtils.join(result, ", ");
    }

    public String getOutputsAsString(OperationDocumentation op) {
        String[] result = getOutputs(op);
        if (result == null || result.length == 0) {
            return "void";
        }
        return StringUtils.join(result, ", ");
    }

    public String getParamDefaultValue(OperationDocumentation.Param param) {
        if (param.values != null && param.values.length > 0) {
            return StringUtils.join(param.values, ", ");
        }
        return "";
    }

    public boolean hasOperation(OperationDocumentation op) {
        if (op == null) {
            throw new IllegalArgumentException("Operation must not be null");
        }
        if (op.getOperations() == null || op.getOperations().length == 0) {
            return false;
        }
        return true;
    }
}
