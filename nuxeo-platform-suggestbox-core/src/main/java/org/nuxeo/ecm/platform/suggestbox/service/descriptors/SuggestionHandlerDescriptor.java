package org.nuxeo.ecm.platform.suggestbox.service.descriptors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * XMap descriptor for registering overridable suggestion handlers (individual
 * operations or named chains of operations).
 * 
 * @author ogrisel
 */
@XObject("suggestionHandler")
public class SuggestionHandlerDescriptor implements Cloneable {

    @XNode("@name")
    protected String name = "default";

    @XNode("@type")
    protected String type;

    @XNode("@suggesterGroup")
    protected String suggestGroup;

    @XNode("@operation")
    protected String operation;

    @XNode("@operationChain")
    protected String operationChain;
    
    @XNode("@enabled")
    protected boolean enabled = true;

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getType() {
        return type;
    }

    public String getSuggesterGroup() {
        return suggestGroup;
    }

    public String getOperation() {
        return operation;
    }

    public String getOperationChain() {
        return operationChain;
    }

    /*
     * Override the Object.clone to make it public
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
