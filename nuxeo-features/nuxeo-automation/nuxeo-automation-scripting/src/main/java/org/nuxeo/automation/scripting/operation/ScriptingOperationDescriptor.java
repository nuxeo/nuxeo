package org.nuxeo.automation.scripting.operation;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.automation.OperationDocumentation;

@XObject("scriptedOperation")
public class ScriptingOperationDescriptor {

    @XNode("@id")
    protected String id;

    @XNode("inputType")
    protected String inputType;

    @XNode("outputType")
    protected String outputType;

    @XNode("description")
    protected String description;

    @XNode("category")
    protected String category;

    @XNodeList(value = "aliases/alias", type = String[].class, componentType = String.class)
    protected String[] aliases;

    @XNodeList(value = "param", type = OperationDocumentation.Param[].class, componentType = OperationDocumentation.Param.class)
    protected OperationDocumentation.Param[] params = new OperationDocumentation.Param[0];

    @XNode("script")
    protected String script;

    public String[] getAliases() {
        return aliases;
    }

    public String getInputType() {
        return inputType;
    }

    public String getOutputType() {
        return outputType;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public OperationDocumentation.Param[] getParams() {
        return params;
    }

    public String getScript() {
        return script;
    }

}
