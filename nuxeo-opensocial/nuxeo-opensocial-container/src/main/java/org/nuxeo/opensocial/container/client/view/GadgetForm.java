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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.opensocial.container.client.ContainerConstants;
import org.nuxeo.opensocial.container.client.ContainerEntryPoint;
import org.nuxeo.opensocial.container.client.ContainerMessages;
import org.nuxeo.opensocial.container.client.JsLibrary;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;
import org.nuxeo.opensocial.container.client.bean.PreferencesBean;
import org.nuxeo.opensocial.container.client.bean.ValuePair;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.data.SimpleStore;
import com.gwtext.client.data.Store;
import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.ColorPalette;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.Window;
import com.gwtext.client.widgets.event.ButtonListenerAdapter;
import com.gwtext.client.widgets.event.WindowListenerAdapter;
import com.gwtext.client.widgets.form.Checkbox;
import com.gwtext.client.widgets.form.ComboBox;
import com.gwtext.client.widgets.form.Field;
import com.gwtext.client.widgets.form.FormPanel;
import com.gwtext.client.widgets.form.Label;
import com.gwtext.client.widgets.form.TextArea;
import com.gwtext.client.widgets.form.TextField;

/**
 *
 * @author Guillaume Cusnieux
 *
 */
public class GadgetForm {

  private static final String CLASS_NXDOC_BUTTON = "x-button-form-nxdoc";
  private static final String PREFIX_FRAME = "frame-";
  private static final String CLASS_PICKR = "x-color-palette-pickr";
  private static final String CLASS_PICKR_LABEL = "x-panel-form-label x-color-palette-label";
  private static final String CLASS_NXDOC = "x-field-form-nxdoc";
  private static final String PICKR_KEY = "pickr-";
  private static final String NXDOC_KEY = "nxdoc_";
  private static final int PREF_WIDTH_FIELD = 210;
  private final static ContainerMessages MESSAGES = GWT.create(ContainerMessages.class);
  private static final ContainerConstants CONSTANTS = GWT.create(ContainerConstants.class);

  private static enum TYPE {
    STRING, HIDDEN, BOOL, ENUM, LIST, COLOR
  }

  private GadgetPortlet portlet;
  private Window browserWindow;

  public GadgetForm(GadgetPortlet portlet) {
    this.portlet = portlet;
  }

  public void showForm() {
    showFormInWindow();
  }

  private static final Window window = new Window();

  private void showFormInWindow() {
    window.clear();
    window.add(formGenerator());
    window.setTitle(MESSAGES.preferencesGadget(portlet.getTitle()));
    window.setWidth(400);
    window.setModal(true);
    window.show();
    window.syncSize();
    window.addListener(new WindowListenerAdapter() {
      @Override
      public void onClose(Panel panel) {
        JsLibrary.closeBrowserDoc();
        super.onClose(panel);
      }
    });
  }

  private FormPanel formGenerator() {
    GadgetBean gadget = portlet.getGadgetBean();
    final FormPanel form = new FormPanel();
    form.setLabelWidth(100);
    form.setPaddings(10);
    form.setWidth(400);
    form.setFrame(true);
    List<PreferencesBean> userPrefs = gadget.getUserPrefs();
    for (PreferencesBean b : userPrefs) {
      String type = b.getDataType();
      Field input = null;
      if (TYPE.STRING.name()
          .equals(type)) {
        input = inputStringBuilder(b);
        if (b.getName()
            .substring(0, 6)
            .equals(NXDOC_KEY)) {
          input.addClass(CLASS_NXDOC);
          input.setWidth(125);
          input.setId(b.getName());
          form.add(createBrowserDoc(gadget.getRef(), b.getName()));
        }
      } else if (TYPE.HIDDEN.name()
          .equals(type)) {
        input = inputHiddenBuilder(b);
        if (b.getName()
            .substring(0, 6)
            .equals(PICKR_KEY)) {
          Label l = new Label(MESSAGES.getLabel(input.getFieldLabel()));
          ColorPalette c = new ColorPalette();
          c.addListener(new ColorListener(input));
          c.setTitle(CONSTANTS.colorChoice());
          c.setValue(input.getValueAsString());
          l.setCls(CLASS_PICKR_LABEL);
          c.setCls(CLASS_PICKR);
          form.add(l);
          form.add(c);
        }
      } else if (TYPE.LIST.name()
          .equals(type) || TYPE.ENUM.name()
          .equals(type)) {
        Object[][] dataCombo = dataComboBoxBuilder(b.getEnumValues());
        input = comboBoxBuilder(b, dataCombo);
      } else if (TYPE.BOOL.name()
          .equals(type)) {
        input = checkBoxBuilder(b);
      }
      form.add(input);
    }
    addButtons(form, gadget);
    return form;
  }

  private Field checkBoxBuilder(PreferencesBean b) {
    Checkbox c = new Checkbox(b.getDisplayName());
    c.setName(b.getName());
    c.setChecked(Boolean.parseBoolean(getPrefValue(b)));
    return c;
  }

  private Button createBrowserDoc(String ref, String prefName) {
    final String url = JsLibrary.getSearchBrowserUrl(ref, prefName);

    browserWindow = new Window();
    browserWindow.setModal(true);
    browserWindow.setClosable(true);
    browserWindow.setWidth(700);
    browserWindow.setHeight(610);
    browserWindow.setPlain(true);
    browserWindow.setCloseAction(Window.HIDE);

    Panel p = new Panel();
    final String id = PREFIX_FRAME + prefName;
    p.setId(id);

    browserWindow.add(p);

    Button button = new Button(CONSTANTS.search());
    button.addListener(new ButtonListenerAdapter() {
      public void onClick(Button button, EventObject e) {
        browserWindow.show(button.getId());
        JsLibrary.showSearchBrowser(id, url);
      }
    });
    button.addClass(CLASS_NXDOC_BUTTON);
    return button;
  }

  public void closeSearchBrowser() {
    if (browserWindow != null)
      browserWindow.close();
  }

  private Object[][] dataComboBoxBuilder(List<ValuePair> enumValues) {
    ArrayList<Object[]> list = new ArrayList<Object[]>();
    for (ValuePair vPair : enumValues) {
      list.add(new Object[] { vPair.getValue(), vPair.getDisplayValue() });
    }
    return list.toArray(new Object[list.size()][2]);
  }

  private TextField comboBoxBuilder(PreferencesBean bean, Object[][] dataCombo) {
    ComboBox cb = new ComboBox(bean.getDisplayName(), bean.getName());
    String _key = "key";
    String _value = "value";
    final Store store = new SimpleStore(new String[] { _key, _value },
        dataCombo);
    store.load();
    cb.setStore(store);
    cb.setForceSelection(true);
    cb.setWidth(PREF_WIDTH_FIELD);
    cb.setDisplayField(_value);
    cb.setValueField(_key);
    cb.setValue(getPrefValue(bean));
    cb.setMode(ComboBox.LOCAL);
    cb.setTriggerAction(ComboBox.ALL);
    cb.setSelectOnFocus(true);
    cb.setEditable(false);
    cb.setHideTrigger(false);
    cb.setReadOnly(true);
    cb.setLinked(true);
    cb.autoSize();
    return cb;
  }

  private TextField inputHiddenBuilder(PreferencesBean bean) {
    TextArea ar = new TextArea(bean.getDisplayName(), bean.getName());
    ar.setValue(bean.getValue());
    ar.hide();
    return ar;
  }

  private TextField inputStringBuilder(PreferencesBean bean) {
    TextField in = new TextField(bean.getDisplayName(), bean.getName(),
        PREF_WIDTH_FIELD, getPrefValue(bean));
    return in;
  }

  private String getPrefValue(PreferencesBean bean) {
    String value = bean.getValue();
    if (value == null)
      value = bean.getDefaultValue();
    String decode = URL.decode(value);
    return decode;
  }

  private void addButtons(final FormPanel form, final GadgetBean gadget) {
    Button save = new Button(CONSTANTS.save());
    save.addListener(new ButtonListenerAdapter() {
      public void onClick(Button button, EventObject e) {
        savePreferences(form.getForm()
            .getValues(), gadget);
      }
    });
    form.addButton(save);

    Button cancel = new Button(CONSTANTS.cancel());
    cancel.addListener(new ButtonListenerAdapter() {
      public void onClick(Button button, EventObject e) {
        window.close();
      }
    });
    form.addButton(cancel);
  }

  private void savePreferences(final String params, final GadgetBean gadget) {
    ContainerEntryPoint.getService()
        .saveGadgetPreferences(gadget, params,
            ContainerEntryPoint.getGwtParams(),
            new SavePreferenceAsyncCallback<GadgetBean>(gadget));
    JsLibrary.loadingShow();
    window.close();
  }
}
