package org.nuxeo.ecm.platform.audit.ws;

import java.io.Serializable;

public class ModifiedDocumentDescriptorPage implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final int pageIndex;

    private final boolean bHasMorePage;

    private ModifiedDocumentDescriptor[] modifiedDocuments;

    public ModifiedDocumentDescriptorPage(ModifiedDocumentDescriptor[] data, int pageIndex, boolean bHasModePage)
    {
        this.pageIndex=pageIndex;
        this.bHasMorePage=bHasModePage;
        this.modifiedDocuments=data;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public boolean hasMorePage() {
        return bHasMorePage;
    }

    public ModifiedDocumentDescriptor[] getModifiedDocuments() {
        return modifiedDocuments;
    }


    public boolean getHasMorePage() {
        return bHasMorePage;
    }

}
