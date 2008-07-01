package org.nuxeo.ecm.platform.ui.flex.samples;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.flex.javadto.FlexDocumentModel;

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

    @WebRemote
    public DummyBean testSerialization() {

        DummyBean test = new DummyBean();
        test.setMyField("this field was set from Seam");

        return test;
    }


    @WebRemote
    public FlexDocumentModel getTestDocumentModel()
    {
        FlexDocumentModel doc = new FlexDocumentModel();

        Map<String, Serializable> schemadata = new HashMap<String, Serializable>();

        schemadata.put("title", "I am a test");

        doc.feed("dublincore", schemadata);

        return doc;
    }

}
