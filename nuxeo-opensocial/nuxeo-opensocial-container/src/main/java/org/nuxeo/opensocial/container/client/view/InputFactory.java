package org.nuxeo.opensocial.container.client.view;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.opensocial.container.client.ContainerConstants;
import org.nuxeo.opensocial.container.client.ContainerMessages;
import org.nuxeo.opensocial.container.client.bean.PreferencesBean;
import org.nuxeo.opensocial.container.client.bean.ValuePair;
import org.nuxeo.opensocial.container.client.view.rest.NXIDPreference;
import org.nuxeo.opensocial.container.client.view.rest.NXRequestCallback;
import org.nuxeo.opensocial.container.client.view.rest.NXRestAPI;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.URL;
import com.gwtext.client.core.ListenerConfig;
import com.gwtext.client.data.SimpleStore;
import com.gwtext.client.data.Store;
import com.gwtext.client.widgets.BoxComponent;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.form.Checkbox;
import com.gwtext.client.widgets.form.ComboBox;
import com.gwtext.client.widgets.form.Field;
import com.gwtext.client.widgets.form.Label;
import com.gwtext.client.widgets.form.TextArea;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.form.event.FieldListenerAdapter;

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

    private static final int PREF_WIDTH_FIELD = 232;

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
        if (TYPES.isColor(b.getDataType(), b.getName())) {
            return new NXFieldColor(gp, b);
        } else if (TYPES.isComboColor(b.getDataType(), b.getName())) {
            return new NXFieldColorCombo(gp, b);
        } else if (TYPES.isNxId(b.getDataType(), b.getName())) {
            return new NXFieldId(gp, b);
        } else if (TYPES.isString(b.getDataType())) {
            return new NXField(gp, b);
        } else if (TYPES.isHidden(b.getDataType())) {
            return new NXFieldHidden(b);
        } else if (TYPES.isCombo(b.getDataType())) {
            return new NXFieldComboBox(b);
        } else if (TYPES.isBool(b.getDataType())) {
            return new NXFieldCheckbox(b);
        }
        return null;
    }

    /****************************************************/
    /** Field Input Text **/

    private class NXField extends TextField {

        public NXField(GadgetPortlet gp, PreferencesBean bean) {
            this.setLabel(bean.getDisplayName());
            this.setName(bean.getName());
            this.setWidth(PREF_WIDTH_FIELD);
            if (bean.getName()
                    .equals("title")) {
                this.setValue(gp.getTitle());
                ListenerConfig config = new ListenerConfig();
                config.setDelay(200);
                this.addKeyPressListener(new NXEventCallback(gp, this), config);
            } else
                this.setValue(getPrefValue(bean));
        }
    }

    /****************************************************/
    /** Field NX ID **/

    private class NXFieldId extends Panel {

        public NXFieldId(GadgetPortlet gp, PreferencesBean bean) {
            Label label = new Label(bean.getDisplayName());
            label.addClass("x-form-item-label");
            this.add(label);
            TextField field = new TextField();
            field.setLabel(bean.getDisplayName());
            field.setWidth(PREF_WIDTH_FIELD);
            field.addClass("x-form-fieldid");
            this.add(field);
            TextArea area = new TextArea();
            area.setLabel(bean.getDisplayName());
            String name = bean.getName();
            area.setName(name);

            final String type = name.substring(5);

            NXIDPreference nxIDPreference = new NXIDPreference(
                    getPrefValue(bean));

            field.setValue(nxIDPreference.getName());
            area.setValue(nxIDPreference.toString());

            area.hide();

            final RequestCallback callback = new NXRequestCallback(field, area,
                    type);
            this.add(area);

            field.addListener(new FieldListenerAdapter() {
                @Override
                public void onFocus(Field field) {
                    NXRestAPI.queryDocType(type, 0, callback);
                }
            });
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
            palette.addListener(new ColorListener(gp, bean.getName(),
                    fieldHidden));
            palette.setTitle(CONSTANTS.colorChoice());
            label.setCls(CSS_CLS.COLOR_LBL.toString());
            palette.setCls(CSS_CLS.COLOR.toString());
            palette.select(bean.getValue());
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
                list.add(new Object[] { vPair.getValue(),
                        vPair.getDisplayValue() });
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

    public static enum TYPES {
        STRING, HIDDEN, BOOL, ENUM, LIST, COLOR_, NXID_;

        public static boolean isString(String type) {
            return STRING.name()
                    .equals(type);
        }

        public static boolean isNxId(String dataType, String name) {
            return isString(dataType) && name.substring(0, NXID_.name()
                    .length())
                    .equals(NXID_.name());
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
        if (bean.getDefaultValue()
                .equals("none"))
            return "";
        if (value == null)
            value = bean.getDefaultValue();
        String decode = URL.decode(value);
        return decode;
    }

}
