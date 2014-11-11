package org.nuxeo.runtime.api;

import java.rmi.dgc.VMID;

/**
*
* Provides a way to identify a Nuxeo Runtime instance.
*
* Identifier can be :
*
*  - automatically generated (default) based on a {@link VMID}
*  - explicitly set as a system property (org.nuxeo.runtime.instance.id)
*
* @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
*
*/
public class RuntimeInstanceIdentifier {

    protected static VMID vmid = new VMID();

    protected static String id;

    public static final String INSTANCE_ID_PROPERTY_NAME = "org.nuxeo.runtime.instance.id";

    public static String getId() {
        if (id==null) {
            id= Framework.getProperty(INSTANCE_ID_PROPERTY_NAME, getVmid().toString());
        }
        return id;
    }

    public static VMID getVmid() {
        return vmid;
    }

}
