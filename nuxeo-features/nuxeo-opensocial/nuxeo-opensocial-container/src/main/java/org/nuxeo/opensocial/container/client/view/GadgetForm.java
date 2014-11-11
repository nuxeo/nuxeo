/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.container.client.view;

import java.util.List;

import org.nuxeo.opensocial.container.client.ContainerConstants;
import org.nuxeo.opensocial.container.client.ContainerEntryPoint;
import org.nuxeo.opensocial.container.client.ContainerMessages;
import org.nuxeo.opensocial.container.client.JsLibrary;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;
import org.nuxeo.opensocial.container.client.bean.PreferencesBean;

import com.google.gwt.core.client.GWT;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.Window;
import com.gwtext.client.widgets.event.ButtonListenerAdapter;
import com.gwtext.client.widgets.form.FormPanel;

/**
 *
 * @author Guillaume Cusnieux
 *
 */
public class GadgetForm {

    private final static ContainerMessages MESSAGES = GWT.create(ContainerMessages.class);

    private static final ContainerConstants CONSTANTS = GWT.create(ContainerConstants.class);

    private String title;

    private GadgetBean gadget;

    private GadgetPortlet portlet;

    public static final Window window = new Window();

    public GadgetForm(GadgetPortlet portlet) {
        this.portlet = portlet;
        this.gadget = portlet.getGadgetBean();
        this.title = portlet.getTitle();
    }

    public void showForm() {
        FormPanel form = new FormPanel();
        form.setLabelWidth(100);
        form.setPaddings(10);
        form.setWidth("100%");
        form.setFrame(true);
        createFields(form);
        this.setTitle(title);
        window.clear();
        window.add(form);
        window.setWidth(400);
        window.setModal(true);
        window.show();
        window.syncSize();
    }

    private FormPanel createFields(FormPanel form) {
        addFields(form, this.gadget.getDefaultPrefs());
        Panel p = new Panel("", "");
        p.addClass("form-separator");
        form.add(p);
        addFields(form, this.gadget.getUserPrefs());
        return addButtons(form);
    }

    private void addFields(Panel p, List<PreferencesBean> prefs) {
        for (PreferencesBean b : prefs) {
            p.add(InputFactory.getInstance().createField(portlet, b));
        }
    }

    private FormPanel addButtons(final FormPanel form) {
        Button save = new Button(CONSTANTS.save());

        save.addListener(new ButtonListenerAdapter() {
            public void onClick(Button button, EventObject e) {
                savePreferences(form);
            }
        });
        form.addButton(save);

        Button cancel = new Button(CONSTANTS.cancel());
        cancel.addListener(new ButtonListenerAdapter() {
            public void onClick(Button button, EventObject e) {
                portlet.renderDefaultPreferences();
                portlet.renderTitle();
                window.close();
            }
        });
        form.addButton(cancel);
        return form;
    }

    private void savePreferences(FormPanel form) {
        ContainerEntryPoint.getService().saveGadgetPreferences(gadget,
                form.getForm().getValues(), ContainerEntryPoint.getGwtParams(),
                new SavePreferenceAsyncCallback<GadgetBean>(gadget));
        JsLibrary.loadingShow();
        window.close();
    }

    public void setTitle(String title) {
        this.title = title;
        window.setTitle(MESSAGES.preferencesGadget((title != null) ? title : ""));
    }

    public void setGadget(GadgetBean gadget) {
        this.gadget = gadget;
    }

}
