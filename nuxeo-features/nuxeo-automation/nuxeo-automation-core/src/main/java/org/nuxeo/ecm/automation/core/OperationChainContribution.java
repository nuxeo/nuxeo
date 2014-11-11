/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core;

import static org.nuxeo.ecm.automation.core.Constants.T_BOOLEAN;
import static org.nuxeo.ecm.automation.core.Constants.T_DATE;
import static org.nuxeo.ecm.automation.core.Constants.T_DOCUMENT;
import static org.nuxeo.ecm.automation.core.Constants.T_DOCUMENTS;
import static org.nuxeo.ecm.automation.core.Constants.T_FLOAT;
import static org.nuxeo.ecm.automation.core.Constants.T_INTEGER;
import static org.nuxeo.ecm.automation.core.Constants.T_PROPERTIES;
import static org.nuxeo.ecm.automation.core.Constants.T_RESOURCE;
import static org.nuxeo.ecm.automation.core.Constants.T_STRING;

import java.net.URL;
import java.util.ArrayList;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.core.impl.adapters.StringToDocRef;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.impl.DocumentRefListImpl;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("chain")
public class OperationChainContribution {

    @XNode("@id")
    protected String id;

    @XNode("@replace")
    protected boolean replace;

    @XNode("description")
    protected String description;

    @XNodeList(value = "operation", type = ArrayList.class, componentType = Operation.class)
    protected ArrayList<Operation> ops;

    @XNode("public")
    protected boolean isPublic = true;

    public OperationChain toOperationChain(Bundle bundle) throws Exception {
        OperationChain chain = new OperationChain(id);
        chain.setDescription(description);
        chain.setPublic(isPublic);
        for (Operation op : ops) {
            OperationParameters params = chain.add(op.id);
            for (Param param : op.params) {
                param.value = param.value.trim();
                if (param.value.startsWith("expr:")) {
                    param.value = param.value.substring(5);
                    // decode < and >
                    param.value = param.value.replaceAll("&lt;", "<");
                    param.value = param.value.replaceAll("&gt;", ">");
                    if (param.value.contains("@{")) {
                        params.set(param.name,
                                Scripting.newTemplate(param.value));
                    } else {
                        params.set(param.name,
                                Scripting.newExpression(param.value));
                    }
                } else {
                    Object val = null;
                    String type = param.type.toLowerCase();
                    char c = type.charAt(0);
                    switch (c) {
                    case 's': // string
                        if (T_STRING.equals(type)) {
                            val = param.value;
                        }
                        break;
                    case 'p':
                        if (T_PROPERTIES.equals(type)) {
                            val = new Properties(param.value);
                        }
                        break;
                    case 'i':
                        if (T_INTEGER.equals(type)) {
                            val = Long.valueOf(param.value);
                        }
                        break;
                    case 'b':
                        if (T_BOOLEAN.equals(type)) {
                            val = Boolean.valueOf(param.value);
                        }
                        break;
                    case 'd':
                        if (T_DOCUMENT.equals(type)) {
                            if (param.value.startsWith(".")) {
                                val = Scripting.newExpression("Document.resolvePathAsRef(\""
                                        + param.value + "\")");
                            } else {
                                val = StringToDocRef.createRef(param.value);
                            }
                        } else if (T_DOCUMENTS.equals(type)) {
                            String[] ar = StringUtils.split(param.value, ',',
                                    true);
                            DocumentRefListImpl result = new DocumentRefListImpl(
                                    ar.length);
                            for (String ref : ar) {
                                result.add(StringToDocRef.createRef(ref));
                            }
                            val = result;
                        } else if (T_DATE.equals(type)) {
                            val = DateParser.parseW3CDateTime(param.value);
                        }
                        break;
                    case 'f':
                        if (T_FLOAT.equals(type)) {
                            val = Double.valueOf(param.value);
                        }
                        break;
                    case 'r':
                        if (T_RESOURCE.equals(type)) {
                            if (param.value.contains(":/")) { // a real URL
                                val = new URL(param.value);
                            } else { // try with class loader
                                val = bundle.getEntry(param.value);
                            }
                        }
                        break;
                    }
                    if (val == null) {
                        val = param.value;
                    }
                    params.set(param.name, val);
                }
            }
        }
        return chain;
    }

    @XObject("operation")
    public static class Operation {
        @XNode("@id")
        protected String id;

        @XNodeList(value = "param", type = ArrayList.class, componentType = Param.class)
        protected ArrayList<Param> params;
    }

    @XObject("param")
    public static class Param {
        @XNode("@name")
        protected String name;

        // string, boolean, date, integer, float, uid, path, expression,
        // template, resource
        @XNode("@type")
        protected String type = "string";

        @XContent
        protected String value;
    }

}
