/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.jaxrs.doc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.view.TemplateView;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocResource {

    @Context
    protected UriInfo uri;

    protected AutomationService service;

    protected List<OperationDocumentation> ops;

    public DocResource() {
        try {
            service = Framework.getService(AutomationService.class);
            ops = service.getDocumentation();
        } catch (Exception e) {
            throw WebException.wrap("Failed to get automation service", e);
        }
    }

    protected TemplateView getTemplate() {
        return getTemplate("index.ftl");
    }

    protected TemplateView getTemplate(String name) {
        Map<String, List<OperationDocumentation>> cats = new LinkedHashMap<String, List<OperationDocumentation>>();
        for (OperationDocumentation op : ops) {
            List<OperationDocumentation> list = cats.get(op.getCategory());
            if (list == null) {
                list = new ArrayList<OperationDocumentation>();
                cats.put(op.getCategory(), list);
            }
            list.add(op);
        }
        return new TemplateView(this, name).arg("categories", cats).arg(
                "operations", ops);
    }

    @GET
    public Object doGet(@QueryParam("id")
    String id) {
        if (id == null) {
            return getTemplate();
        } else {
            OperationDocumentation opDoc = null;
            for (OperationDocumentation op : ops) {
                if (op.getId().equals(id)) {
                    opDoc = op;
                    break;
                }
            }
            if (opDoc == null) {
                throw new WebResourceNotFoundException(
                        "No operation found with name: " + id);
            }
            TemplateView tpl = getTemplate();
            tpl.arg("operation", opDoc);
            return tpl;
        }
    }

    @GET
    @Path("wiki")
    public Object doGetWiki() {
        return getTemplate("wiki.ftl");
    }

    public String[] getInputs(OperationDocumentation op) {
        if (op.signature == null && op.signature.length == 0) {
            return new String[0];
        }
        String[] result = new String[op.signature.length / 2];
        for (int i = 0, k = 0; i < op.signature.length; i += 2, k++) {
            result[k] = op.signature[i];
        }
        return result;
    }

    public String[] getOutputs(OperationDocumentation op) {
        if (op.signature == null && op.signature.length == 0) {
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

}
