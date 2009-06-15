package org.nuxeo.ecm.platform.ui.web.seamremoting;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject(value = "remotableSeamBeans")
public class RemotableSeamBeansDescriptor implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @XNodeList(value = "beans/bean", type = ArrayList.class, componentType = String.class)
    private List<String> beanNames;

    public List<String> getBeanNames() {
        return beanNames;
    }

}
