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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation.io.yaml;

import static org.nuxeo.ecm.automation.core.Constants.T_PROPERTIES;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationDocumentation.Param;
import org.nuxeo.ecm.automation.core.OperationChainContribution;
import org.nuxeo.ecm.automation.core.OperationChainContribution.Operation;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

/**
 * YAML representer for automation chains.
 *
 * @since 5.9.4
 */
public class YamlAutomationRepresenter extends Representer {

    public YamlAutomationRepresenter() {
        super();
        this.addClassTag(OperationDocumentation.class, Tag.MAP);
        this.representers.put(OperationDocumentation.class, new ChainRepr());
        this.addClassTag(Operation.class, Tag.MAP);
        this.representers.put(Operation.class, new OpRepr());
        this.addClassTag(Param.class, Tag.MAP);
        this.representers.put(Param.class, new ParamRepr());
    }

    public class ChainRepr implements Represent {
        @Override
        public Node representData(Object data) {
            OperationDocumentation c = (OperationDocumentation) data;
            Tag tag = getTag(OperationDocumentation.class, null);
            Map<Object, Object> mapping = new LinkedHashMap<Object, Object>();
            if (c.getDescription() != null) {
                mapping.put("description", c.getDescription());
            }
            if (c.getParams() != null && c.getParams().length != 0) {
                mapping.put("params", c.getParams());
            }
            Operation[] ops = c.getOperations();
            if (ops != null && ops.length != 0) {
                if (mapping.isEmpty()) {
                    // omit the "operations" parent if params and description
                    // are empty
                    return YamlAutomationRepresenter.this.representData(ops);
                }
                mapping.put("operations", ops);
            }
            return representMapping(tag, mapping, FlowStyle.AUTO);
        }
    }

    public class OpRepr implements Represent {

        @Override
        public Node representData(Object data) {
            Operation op = (Operation) data;
            Tag tag = getTag(Operation.class, null);
            List<OperationChainContribution.Param> params = op.getParams();
            if (params != null && !params.isEmpty()) {
                Map<Object, Object> mapping = new HashMap<Object, Object>();
                Map<Object, Object> subs = new LinkedHashMap<Object, Object>();
                for (OperationChainContribution.Param param : params) {
                    // handle java properties use case
                    if (T_PROPERTIES.equals(param.getType())) {
                        if (param.getMap() != null && !param.getMap().isEmpty()) {
                            Properties props = new Properties(param.getMap());
                            subs.put(param.getName(), props);
                        } else {
                            try {
                                Properties props = new Properties(param.getValue());
                                subs.put(param.getName(), props);
                            } catch (IOException e) {
                                subs.put(param.getName(), param.getValue());
                            }
                        }
                    } else {
                        subs.put(param.getName(), param.getValue());
                    }
                }
                mapping.put(op.getId(), subs);
                return representMapping(tag, mapping, FlowStyle.AUTO);
            } else {
                return YamlAutomationRepresenter.this.representData(op.getId());
            }
        }
    }

    public class ParamRepr implements Represent {
        @Override
        public Node representData(Object data) {
            Param p = (Param) data;
            Tag tag = getTag(Param.class, null);
            Map<Object, Object> mapping = new HashMap<Object, Object>();
            Map<Object, Object> subs = new LinkedHashMap<Object, Object>();
            subs.put("type", p.getType());
            if (p.getDescription() != null) {
                subs.put("description", p.getDescription());
            }
            if (p.getValues() != null && p.getValues().length != 0) {
                subs.put("values", p.getValues());
            }
            mapping.put(p.getName(), subs);
            return representMapping(tag, mapping, FlowStyle.AUTO);
        }
    }

}
