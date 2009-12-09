package org.nuxeo.opensocial.container.client.view;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.opensocial.container.client.ContainerConstants;
import org.nuxeo.opensocial.container.client.ContainerMessages;
import org.nuxeo.opensocial.container.client.bean.PreferencesBean;
import org.nuxeo.opensocial.container.client.bean.ValuePair;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.gwtext.client.data.SimpleStore;
import com.gwtext.client.data.Store;
import com.gwtext.client.widgets.BoxComponent;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.form.Checkbox;
import com.gwtext.client.widgets.form.ComboBox;
import com.gwtext.client.widgets.form.Label;
import com.gwtext.client.widgets.form.TextArea;
import com.gwtext.client.widgets.form.TextField;

public class InputFactory {

  private static enum CSS_CLS {
    COLOR("x-color-palette"), COLOR_LBL("x-color-palette-label"), BROWS(
        "x-field-form-nxdoc"), BROWS_BUT("x-button-form-nxdoc"), COLOR_PAN(
        "x-panel-palette");

    private String className;

    private CSS_CLS(String className) {
      this.className = className;
    }

    @Override
    public String toString() {
      return className;
    }

  }

  private static final int PREF_WIDTH_FIELD = 210;

  private static final ContainerConstants CONSTANTS = GWT.create(ContainerConstants.class);
  private final static ContainerMessages MESSAGES = GWT.create(ContainerMessages.class);

  private static InputFactory singleton = null;

  private InputFactory() {
  }

  public static InputFactory getInstance() {
    if (singleton == null)
      singleton = new InputFactory();
    return singleton;
  }

  public BoxComponent createField(GadgetPortlet gp, PreferencesBean b) {
    BoxComponent box = null;
    if (TYPES.isColor(b.getDataType(), b.getName())) {
      box = new NXFieldColor(gp, b);
    } else if (TYPES.isComboColor(b.getDataType(), b.getName())) {
      box = new NXFieldColorCombo(gp, b);
    } else if (TYPES.isString(b.getDataType())) {
      box = new NXField(b);
    } else if (TYPES.isHidden(b.getDataType())) {
      box = new NXFieldHidden(b);
    } else if (TYPES.isCombo(b.getDataType())) {
      box = new NXFieldComboBox(b);
    } else if (TYPES.isBool(b.getDataType())) {
      box = new NXFieldCheckbox(b);
    }
    return box;
  }

  /****************************************************/
  /** Field Input Text **/

  private class NXField extends TextField {

    public NXField(PreferencesBean bean) {
      this.setLabel(bean.getDisplayName());
      this.setName(bean.getName());
      this.setWidth(PREF_WIDTH_FIELD);
      this.setValue(getPrefValue(bean));
    }
  }

  /****************************************************/
  /** Field Input Hidden **/

  private class NXFieldHidden extends TextArea {

    public NXFieldHidden(PreferencesBean bean) {
      this.setLabel(bean.getDisplayName());
      this.setName(bean.getName());
      this.setValue(getPrefValue(bean));
      this.hide();
    }
  }

  private class NXFieldColor extends Panel {

    protected NXColorPalette palette;

    public NXFieldColor(GadgetPortlet gp, PreferencesBean bean) {
      super();
      NXFieldHidden fieldHidden = new NXFieldHidden(bean);
      Label label = new Label(MESSAGES.getLabel(bean.getDisplayName()));
      palette = new NXColorPalette();
      palette.addListener(new ColorListener(gp, bean.getName(), fieldHidden));
      palette.setTitle(CONSTANTS.colorChoice());
      label.setCls(CSS_CLS.COLOR_LBL.toString());
      palette.setCls(CSS_CLS.COLOR.toString());
      this.add(fieldHidden);
      this.add(label);
      this.add(palette);
      this.addClass(CSS_CLS.COLOR_PAN.toString());
    }
  }

  private class NXFieldColorCombo extends NXFieldColor {

    public NXFieldColorCombo(GadgetPortlet gp, PreferencesBean bean) {
      super(gp, bean);
      palette.setColors(colorsBuilder(bean));
    }

    private String[] colorsBuilder(PreferencesBean b) {
      List<String> l = new ArrayList<String>();
      for (ValuePair v : b.getEnumValues()) {
        l.add(v.getValue());
      }
      return l.toArray(new String[l.size()]);
    }
  }

  /****************************************************/
  /** Field Input Combo **/

  private class NXFieldComboBox extends ComboBox {

    private static final String _KEY = "key";
    private static final String _VALUE = "value";
    private PreferencesBean bean;

    public NXFieldComboBox(PreferencesBean bean) {
      this.bean = bean;
      this.setLabel(bean.getDisplayName());
      this.setName(bean.getName());
      Store store = new SimpleStore(new String[] { _KEY, _VALUE },
          dataComboBoxBuilder());
      store.load();
      this.setStore(store);
      this.setForceSelection(true);
      this.setWidth(PREF_WIDTH_FIELD);
      this.setDisplayField(_VALUE);
      this.setValueField(_KEY);
      this.setValue(getPrefValue(bean));
      this.setMode(ComboBox.LOCAL);
      this.setTriggerAction(ComboBox.ALL);
      this.setSelectOnFocus(true);
      this.setEditable(false);
      this.setHideTrigger(false);
      this.setReadOnly(true);
      this.setLinked(true);
      this.autoSize();
    }

    private Object[][] dataComboBoxBuilder() {
      ArrayList<Object[]> list = new ArrayList<Object[]>();
      for (ValuePair vPair : this.bean.getEnumValues()) {
        list.add(new Object[] { vPair.getValue(), vPair.getDisplayValue() });
      }
      return list.toArray(new Object[list.size()][2]);
    }
  }

  /****************************************************/
  /** Field Input Checkbox **/

  private class NXFieldCheckbox extends Checkbox {

    public NXFieldCheckbox(PreferencesBean bean) {
      this.setLabel(bean.getDisplayName());
      this.setName(bean.getName());
      this.setChecked(Boolean.parseBoolean(getPrefValue(bean)));
    }
  }

  private static enum TYPES {
    STRING, HIDDEN, BOOL, ENUM, LIST, COLOR_, BROWS_;

    public static boolean isString(String type) {
      return STRING.name()
          .equals(type);
    }

    public static boolean isHidden(String type) {
      return HIDDEN.name()
          .equals(type);
    }

    public static boolean isCombo(String type) {
      return LIST.name()
          .equals(type) || ENUM.name()
          .equals(type);
    }

    public static boolean isBool(String type) {
      return BOOL.name()
          .equals(type);
    }

    private static boolean isDefaultColor(String type, String name) {
      return name.substring(0, COLOR_.name()
          .length())
          .equals(COLOR_.name());
    }

    public static boolean isColor(String type, String name) {
      return isHidden(type) && isDefaultColor(type, name);
    }

    public static boolean isComboColor(String type, String name) {
      return isCombo(type) && isDefaultColor(type, name);
    }

  }

  private String getPrefValue(PreferencesBean bean) {
    String value = bean.getValue();
    if (value == null)
      value = bean.getDefaultValue();
    String decode = URL.decode(value);
    return decode;
  }

}
