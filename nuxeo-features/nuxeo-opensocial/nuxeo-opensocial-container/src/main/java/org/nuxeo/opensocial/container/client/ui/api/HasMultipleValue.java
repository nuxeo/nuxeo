package org.nuxeo.opensocial.container.client.ui.api;

import com.google.gwt.user.client.ui.HasValue;

/**
 * @author Stéphane Fourrier
 */
public interface HasMultipleValue<T> extends HasValue<T> {

    void addValue(T item, T value);

    T getValue();

    void setValue(T value);

    void setItemSelected(int index);

    int getItemCount();

}
