package org.nuxeo.opensocial.container.server.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author Stéphane Fourrier
 */
@XObject("webcontentsaver")
public class WebContentSaverDescriptor {

    /**
     * <webcontentsaver doctype="WCHTML"> <type>wchtml</type>
     * <daoClass>HTMLWebContentDAO</daoClass>
     * <coreAdapter>HTMLAdapter</coreAdapter> </webcontentsaver>
     */
    @XNode("@docType")
    private String docType;

    @XNode("@type")
    private String type;

    @XNode("@daoClass")
    private Class<?> daoClass;

    @XNode("@coreAdapter")
    private Class<?> coreAdapter;

    public WebContentSaverDescriptor() {
    }

    public WebContentSaverDescriptor(String docType, String type,
            Class<?> daoClass, Class<?> coreAdapter) {
        setDocType(docType);
        setType(type);
        setDaoClass(daoClass);
        setCoreAdapter(coreAdapter);
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getDocType() {
        return docType;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setDaoClass(Class<?> daoClass) {
        this.daoClass = daoClass;
    }

    public Class<?> getDaoClass() {
        return daoClass;
    }

    public void setCoreAdapter(Class<?> coreAdapter) {
        this.coreAdapter = coreAdapter;
    }

    public Class<?> getCoreAdapter() {
        return coreAdapter;
    }
}
