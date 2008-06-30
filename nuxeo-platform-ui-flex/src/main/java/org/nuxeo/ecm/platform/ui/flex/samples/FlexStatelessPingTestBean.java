package org.nuxeo.ecm.platform.ui.flex.samples;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

@Name("flexStatelessPingTestBean")
@Scope(ScopeType.STATELESS)
public class FlexStatelessPingTestBean implements Serializable {

    /**
     *
     */

    @In(create=false,required=false)
    NuxeoPrincipal flexUser;

    private static final long serialVersionUID = 1L;

    @WebRemote
    public String ping() {
        return "Hello from stateless Nuxeo Seam Bean";
    }


    @WebRemote
    public String pingUser() {

        if (flexUser==null)
            return "Stateless Nuxeo Seam bean saye Hello to null user";
        else
            return "Stateless Nuxeo Seam bean saye Hello to " + flexUser.getName();
    }

    public void getFirstChildren(String path)
    {


    }

}
