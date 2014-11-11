package org.nuxeo.opensocial.container.client.view;

import com.gwtext.client.core.EventCallback;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.widgets.form.Field;

public class NXEventCallback implements EventCallback {

    private GadgetPortlet gp;

    private Field field;

    public NXEventCallback(GadgetPortlet gp, Field nxField) {
        this.gp = gp;
        this.field = nxField;
    }

    public void execute(EventObject e) {
        gp.setPortletTitle(field.getValueAsString());
    }

}
