package org.nuxeo.ecm.platform.wss.service;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("docTypes")
public class DocTypesDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@leafDocType")
    protected String leafDocType;

    @XNode("@folderishDocType")
    protected String folderishDocType;

    public String getLeafDocType() {
        return leafDocType;
    }

    public String getFolderishDocType() {
        return folderishDocType;
    }

}
