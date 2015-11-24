package org.nuxeo.opensocial.container.client.ui;

import org.nuxeo.opensocial.container.client.ui.api.HasMultipleValue;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.ListBox;

/**
 * @author Stéphane Fourrier
 */
public class CustomListBox extends ListBox implements HasMultipleValue<String> {
    private boolean valueChangeHandlerInitialized;

    public CustomListBox() {
        super();
    }

    public String getValue() {
        return getValue(getSelectedIndex());
    }

    public void setValue(String value) {
        for (int i = 0; i < getItemCount(); i++) {
            if (getValue(i).equals(value)) {
                setSelectedIndex(i);
                break;
            }
        }
    }

    public void setValue(String value, boolean fireEvents) {
        setValue(value);
        if (fireEvents)
            ValueChangeEvent.fire(this, value);
    }

    public HandlerRegistration addValueChangeHandler(
            ValueChangeHandler<String> handler) {
        if (!valueChangeHandlerInitialized) {
            valueChangeHandlerInitialized = true;
            addChangeHandler(new ChangeHandler() {
                public void onChange(ChangeEvent event) {
                    ValueChangeEvent.fire(CustomListBox.this, getValue());
                }
            });
        }
        return addHandler(handler, ValueChangeEvent.getType());
    }

    public void addValue(String item, String value) {
        this.addItem(item, value);
    }

    public void setItemSelected(int index) {
        this.setSelectedIndex(index);
    }
}
