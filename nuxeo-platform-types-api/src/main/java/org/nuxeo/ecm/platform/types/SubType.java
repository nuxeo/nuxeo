package org.nuxeo.ecm.platform.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Type view to display a given document sub-type.
 *
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 *
 */
@XObject("type")
public class SubType implements Serializable {

    private static final long serialVersionUID = 1L;

    List<String> hidden;

    @XNode
    String name;

    public List<String> getHidden() {
        if (hidden == null) {
            hidden = new ArrayList<String>();
        }
        return hidden;
    }

    @XNode("@hidden")
    public void setHidden(String value) {
        String[] hiddenCases = StringUtils.split(value, ',', true);
        hidden = new ArrayList<String>(Arrays.asList(hiddenCases));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
