package org.nuxeo.ecm.platform.filemanager.service.extension;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("unicitySettings")
public class UnicityExtension  implements Serializable {

    private static final long serialVersionUID = 7764225025169187266L;

    public static final List<String> DEFAULT_FIELDS = new ArrayList<String>();

    @XNode("algo")
    protected String algo;

    @XNode("enabled")
    protected Boolean enabled;

    @XNode("computeDigest")
    protected Boolean computeDigest=false;

    @XNodeList(value = "field", type = ArrayList.class, componentType = String.class)
    protected List<String> fields = DEFAULT_FIELDS;

    public String getAlgo() {
        return algo;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public List<String> getFields() {
        return fields;
    }

    public Boolean getComputeDigest() {
        if (enabled)
            return true;
        return computeDigest;
    }


}
