/*
 * (C) Copyright 2012-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     slacoin <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core;

import static org.nuxeo.ecm.automation.core.Constants.T_BOOLEAN;
import static org.nuxeo.ecm.automation.core.Constants.T_DATE;
import static org.nuxeo.ecm.automation.core.Constants.T_DOCUMENT;
import static org.nuxeo.ecm.automation.core.Constants.T_DOCUMENTS;
import static org.nuxeo.ecm.automation.core.Constants.T_FLOAT;
import static org.nuxeo.ecm.automation.core.Constants.T_INTEGER;
import static org.nuxeo.ecm.automation.core.Constants.T_LONG;
import static org.nuxeo.ecm.automation.core.Constants.T_PROPERTIES;
import static org.nuxeo.ecm.automation.core.Constants.T_RESOURCE;
import static org.nuxeo.ecm.automation.core.Constants.T_STRING;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationException;
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
    protected boolean replace = true;

    @XNode("description")
    protected String description;

    @XNodeList(value = "operation", type = Operation[].class, componentType = Operation.class)
    protected Operation[] ops = new Operation[0];

    @XNode("public")
    protected boolean isPublic = true;

    @XNodeList(value = "param", type = OperationDocumentation.Param[].class, componentType = OperationDocumentation.Param.class)
    protected OperationDocumentation.Param[] params = new OperationDocumentation.Param[0];

    /**
     * @since 7.1
     */
    @XNodeList(value = "aliases/alias", type = String[].class, componentType = String.class)
    protected String[] aliases;

    @XObject("operation")
    public static class Operation {
        @XNode("@id")
        protected String id;

        @XNodeList(value = "param", type = ArrayList.class, componentType = Param.class)
        protected List<Param> params;

        public String getId() {
            return id;
        }

        public List<Param> getParams() {
            return params;
        }
    }

    @XObject("param")
    public static class Param {
        @XNode("@name")
        protected String name;

        // string, boolean, date, integer, float, uid, path, expression,
        // template, resource
        @XNode("@type")
        protected String type = "string";

        // why not XNode here? XContent requires to unescape XML entities, see
        // below
        @XContent
        protected String value;

        // Optional map for properties type values
        @XNodeMap(value = "property", key = "@key", type = HashMap.class, componentType = String.class, nullByDefault = true)
        protected Map<String, String> map;

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public String getType() {
            return type;
        }

        public Map<String, String> getMap() {
            return map;
        }
    }

    public OperationDocumentation.Param[] getParams() {
        return params;
    }

    public String getId() {
        return id;
    }

    public OperationChain toOperationChain(Bundle bundle) throws OperationException {
        OperationChain chain = new OperationChain(id);
        chain.setDescription(description);
        chain.setPublic(isPublic);
        chain.setAliases(aliases);
        for (Operation op : ops) {
            OperationParameters params = chain.add(op.id);
            for (Param param : op.params) {
                param.value = param.value.trim();
                // decode XML entities in every case
                param.value = StringEscapeUtils.unescapeXml(param.value);
                if (param.value.startsWith("expr:")) {
                    param.value = param.value.substring(5);
                    if (param.value.contains("@{")) {
                        params.set(param.name, Scripting.newTemplate(param.value));
                    } else {
                        params.set(param.name, Scripting.newExpression(param.value));
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
                            if (param.map != null && !param.map.isEmpty()) {
                                val = new Properties(param.map);
                            } else {
                                try {
                                    val = new Properties(param.value);
                                } catch (IOException e) {
                                    throw new OperationException(e);
                                }
                            }
                        }
                        break;
                    case 'i':
                        if (T_INTEGER.equals(type)) {
                            val = Integer.parseInt(param.value);
                        }
                        break;
                    case 'l':
                        if (T_LONG.equals(type)) {
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
                                val = Scripting.newExpression("Document" + ".resolvePathAsRef(\"" + param.value + "\")");
                            } else {
                                val = StringToDocRef.createRef(param.value);
                            }
                        } else if (T_DOCUMENTS.equals(type)) {
                            String[] ar = StringUtils.split(param.value, ',', true);
                            DocumentRefListImpl result = new DocumentRefListImpl(ar.length);
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
                                try {
                                    val = new URL(param.value);
                                } catch (MalformedURLException e) {
                                    throw new OperationException(e);
                                }
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

    public Operation[] getOps() {
        return ops;
    }

    public String getLabel() {
        return id;
    }

    public String getRequires() {
        return "";
    }

    public String getCategory() {
        return Constants.CAT_CHAIN;
    }

    public String getSince() {
        return "";
    }

    public String getDescription() {
        return description;
    }

    public String[] getAliases() {
        return aliases;
    }
}
