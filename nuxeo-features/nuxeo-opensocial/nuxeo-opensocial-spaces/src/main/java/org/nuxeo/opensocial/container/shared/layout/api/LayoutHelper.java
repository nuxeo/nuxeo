package org.nuxeo.opensocial.container.shared.layout.api;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.opensocial.container.shared.layout.enume.YUISideBarStyle;
import org.nuxeo.opensocial.container.shared.layout.enume.YUISize;
import org.nuxeo.opensocial.container.shared.layout.enume.YUITemplate;
import org.nuxeo.opensocial.container.shared.layout.exception.LayoutException;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIComponentZoneImpl;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIFixedBodySize;
import org.nuxeo.opensocial.container.shared.layout.impl.YUILayoutImpl;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIUnitImpl;

public class LayoutHelper {
    public enum Preset {
        X_1_DEFAULT("x-1-default"), X_2_DEFAULT("x-2-default"), X_2_66_33(
                "x-2-66-33"), X_2_33_66("x-2-33-66"), X_3_DEFAULT("x-3-default"), X_3_HEADER_2COLS(
                "x-3-header2cols"), X_4_FOOTER_3COLS("x-4-footer3cols"), X_4_HEADER_3COLS(
                "x-4-header3cols"), X_4_66_33_50_50("x-4-66-33-50-50"), X_4_50_50_66_33(
                "x-4-50-50-66-33"), X_4_100_66_33_100("x-4-100-66-33-100"), X_4_100_33_66_100(
                "x-4-100-33-66-100"), X_6_50_50_75_25_50_50(
                "x-6-50-50-75-25-50-50"), X_7_33_33_33_75_25_50_50(
                "x-7-33-33-33-75-25-50-50"), X_7_33_33_33_100_33_33_33(
                "x-7-33-33-33-100-33-33-33"), X_7_75_25_50_50_33_33_33(
                "x-7-75-25-50-50-33-33-33");

        private static final Map<String, Preset> stringToEnum = new HashMap<String, Preset>();
        static {
            for (Preset op : values())
                stringToEnum.put(op.getName(), op);
        }

        private String name;

        private Preset(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        /**
         * @param layout
         * @return
         * @throws LayoutException when layout is not a known layout name.
         */
        public static Preset fromString(String layout) throws LayoutException {
            if (!stringToEnum.containsKey(layout)) {
                throw new LayoutException("Unknown layout '" + layout + "'.");
            }
            return stringToEnum.get(layout);
        }

        public YUILayout getLayout() {
            return LayoutHelper.buildLayout(this);
        }
    }

    public static YUILayout buildLayout(String layoutName)
            throws LayoutException {
        return buildLayout(Preset.fromString(layoutName));
    }

    public static YUILayout buildLayout(Preset layoutPreset) {
        switch (layoutPreset) {
        case X_2_DEFAULT:
            return buildLayout(YUITemplate.YUI_ZT_50_50);
        case X_2_66_33:
            return buildLayout(YUITemplate.YUI_ZT_66_33);
        case X_2_33_66:
            return buildLayout(YUITemplate.YUI_ZT_33_66);
        case X_3_DEFAULT:
            return buildLayout(YUITemplate.YUI_ZT_33_33_33);
        case X_3_HEADER_2COLS:
            return buildLayout(YUITemplate.YUI_ZT_100, YUITemplate.YUI_ZT_50_50);
        case X_4_FOOTER_3COLS:
            return buildLayout(YUITemplate.YUI_ZT_33_33_33,
                    YUITemplate.YUI_ZT_100);
        case X_4_HEADER_3COLS:
            return buildLayout(YUITemplate.YUI_ZT_100,
                    YUITemplate.YUI_ZT_33_33_33);
        case X_4_66_33_50_50:
            return buildLayout(YUITemplate.YUI_ZT_66_33,
                    YUITemplate.YUI_ZT_50_50);
        case X_4_50_50_66_33:
            return buildLayout(YUITemplate.YUI_ZT_50_50,
                    YUITemplate.YUI_ZT_66_33);
        case X_4_100_66_33_100:
            return buildLayout(YUITemplate.YUI_ZT_100,
                    YUITemplate.YUI_ZT_66_33, YUITemplate.YUI_ZT_100);
        case X_4_100_33_66_100:
            return buildLayout(YUITemplate.YUI_ZT_100,
                    YUITemplate.YUI_ZT_33_66, YUITemplate.YUI_ZT_100);
        case X_6_50_50_75_25_50_50:
            return buildLayout(YUITemplate.YUI_ZT_50_50,
                    YUITemplate.YUI_ZT_75_25, YUITemplate.YUI_ZT_50_50);
        case X_7_33_33_33_75_25_50_50:
            return buildLayout(YUITemplate.YUI_ZT_33_33_33,
                    YUITemplate.YUI_ZT_75_25, YUITemplate.YUI_ZT_50_50);
        case X_7_33_33_33_100_33_33_33:
            return buildLayout(YUITemplate.YUI_ZT_33_33_33,
                    YUITemplate.YUI_ZT_100, YUITemplate.YUI_ZT_33_33_33);
        case X_7_75_25_50_50_33_33_33:
            return buildLayout(YUITemplate.YUI_ZT_75_25,
                    YUITemplate.YUI_ZT_50_50, YUITemplate.YUI_ZT_33_33_33);
        default:
            return buildLayout(YUITemplate.YUI_ZT_100);
        }
    }

    public static YUILayout buildLayout(YUITemplate... templates) {
        YUILayout layout = getBaseLayout();
        for (YUITemplate template : templates) {
            YUIComponentZoneImpl zone = new YUIComponentZoneImpl(template);
            layout.getContent().addComponent(zone);
            for (int i = 0; i < template.getNumberOfComponents(); i++) {
                zone.addComponent(new YUIUnitImpl());
            }

        }
        return layout;
    }

    private static YUILayout getBaseLayout() {
        YUIFixedBodySize bodySize = new YUIFixedBodySize(
                YUISize.YUI_BS_FULL_PAGE);
        return new YUILayoutImpl(bodySize, false, false,
                YUISideBarStyle.YUI_SB_NO_COLUMN);

    }
}
