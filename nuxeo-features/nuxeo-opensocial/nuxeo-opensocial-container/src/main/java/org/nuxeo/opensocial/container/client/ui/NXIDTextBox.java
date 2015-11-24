package org.nuxeo.opensocial.container.client.ui;

import com.google.gwt.user.client.ui.TextBox;

public class NXIDTextBox extends TextBox {
    private String hiddenValue;

    public NXIDTextBox() {
        super();
    }

    @Override
    public String getValue() {
        String value = "{\"NXID\":\"" + hiddenValue + "\",\"NXNAME\":\""
                + super.getValue() + "\"}";
        return value;
    }

    public void setHiddenValue(String hiddenValue) {
        this.hiddenValue = hiddenValue;
    }

    public String getHiddenValue() {
        return hiddenValue;
    }
}
