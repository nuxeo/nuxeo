package org.nuxeo.ecm.platform.api.ws;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.security.ACE;

public class DocumentSnapshot implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;


    private DocumentProperty[] noBlobProperties;

    private DocumentBlob[] blobProperties;

    private String pathAsString;

    private ACE[] acl ;


    public DocumentSnapshot()
    {

    }

    public DocumentSnapshot(DocumentProperty[] noBlobProperties,  DocumentBlob[] blobProperties, String pathAsString, ACE[] acl)
    {
     this.acl=acl;
     this.noBlobProperties=noBlobProperties;
     this.blobProperties=blobProperties;
     this.pathAsString=pathAsString;
    }

    public DocumentProperty[] getNoBlobProperties() {
        return noBlobProperties;
    }



    public void setNoBlobProperties(DocumentProperty[] noBlobProperties) {
        this.noBlobProperties = noBlobProperties;
    }



    public DocumentBlob[] getBlobProperties() {
        return blobProperties;
    }



    public void setBlobProperties(DocumentBlob[] blobProperties) {
        this.blobProperties = blobProperties;
    }



    public String getPathAsString() {
        return pathAsString;
    }



    public void setPathAsString(String pathAsString) {
        this.pathAsString = pathAsString;
    }



    public ACE[] getAcl() {
        return acl;
    }



    public void setAcl(ACE[] acl) {
        this.acl = acl;
    }


}
