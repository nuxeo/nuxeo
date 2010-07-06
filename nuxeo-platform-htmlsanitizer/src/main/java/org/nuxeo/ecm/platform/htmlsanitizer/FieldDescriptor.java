package org.nuxeo.ecm.platform.htmlsanitizer;

import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject(value = "field")
public class FieldDescriptor {

    @XContent
    private String contentField;

    @XNode("@filter")
    private String filterField;

    @XNode("@filterValue")
    private String filterValue;

    public String getContentField() {
        if (contentField!=null) {
            String result = contentField.trim();
            result=result.replace("\n", "");
            return result;
        }
        return contentField;
    }

    public String getFilterField() {
        return filterField;
    }

    public String getFilterValue() {
        return filterValue;
    }

    @Override
    public String toString() {
        if (filterField!=null) {
            return contentField + "if " + filterField + "=" + filterValue;
        } else {
            return contentField;
        }
    }

}
