package org.nuxeo.ecm.automation.features.upload;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("importOption")
public class ImportOption implements Comparable<ImportOption>{

    @XNode("@name")
    private String name;

    @XNode("@order")
    private Integer order=10;

    @XNode("@category")
    protected String category;

    @XNode("operationId")
    protected String operationId;

    @XNode("label")
    protected String label;

    @XNode("description")
    protected String description;

    @XNode("formUrl")
    protected String formUrl;

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getLabel() {
        if (label==null) {
            return name + ".label";
        }
        return label;
    }

    public String getFormUrl() {
        return formUrl;
    }

    public String getDescription() {
        if (description==null) {
            return name + ".description";
        }
        return description;
    }

    @Override
    public int compareTo(ImportOption o) {
        if (order==null) {
            return 0;
        }
        return order.compareTo(o.order);
    }



}
