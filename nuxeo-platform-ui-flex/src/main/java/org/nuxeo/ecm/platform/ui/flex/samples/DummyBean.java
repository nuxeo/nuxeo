package org.nuxeo.ecm.platform.ui.flex.samples;

import java.io.Serializable;

public class DummyBean implements Serializable {

    protected String myField;
    /**
     *
     */
    private static final long serialVersionUID = 1L;


    public DummyBean()
    {
       myField="default dummy value";
    }

    public String getMyField() {
        return myField;
    }


    public void setMyField(String myField) {
        this.myField = myField;
    }

    public void doSomething()
    {
        // not impl on the server side
    }
}
