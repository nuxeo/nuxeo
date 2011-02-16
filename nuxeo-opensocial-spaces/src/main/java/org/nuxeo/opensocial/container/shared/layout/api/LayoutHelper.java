package org.nuxeo.opensocial.container.shared.layout.api;

import org.nuxeo.opensocial.container.shared.layout.enume.YUISideBarStyle;
import org.nuxeo.opensocial.container.shared.layout.enume.YUISize;
import org.nuxeo.opensocial.container.shared.layout.enume.YUITemplate;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIComponentZoneImpl;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIFixedBodySize;
import org.nuxeo.opensocial.container.shared.layout.impl.YUILayoutImpl;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIUnitImpl;

public class LayoutHelper {
    public enum Preset {
        X_1_DEFAULT("x-1-default"), X_2_DEFAULT("x-2-default"), X_2_66_33(
                "x-2-66-33"), X_2_33_66("x-2-33-66"), X_3_DEFAULT("x-3-default"), X_3_HEADER_2COLS(
                "x-3-header-2cols"), X_4_FOOTER_3COLS("x-4-footer_3cols"), X_4_HEADER_3COLS(
                "x-4-header-3cols"), X_4_66_33_50_50("x-4-66-33-50-50"), X_4_50_50_66_33(
                "x-4-50-50-66-33"), X_4_100_66_33_100("x-4-100-66-33-100"), X_4_100_33_66_100(
                "x-4-100-33-66-100");

        private String name;

        private Preset(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public YUILayout getLayout() {
            return LayoutHelper.buildLayout(this);
        }
    }

    public static YUILayout buildLayout(String layoutName) throws Exception {
        for (Preset preset : Preset.values()) {
            if (preset.getName().equals(layoutName)) {
                return buildLayout(preset);
            }
        }
        throw new Exception("Layout not found");
    }

    public static YUILayout buildLayout(Preset layoutPreset) {
        switch (layoutPreset) {
        case X_2_DEFAULT:
            return build(YUITemplate.YUI_ZT_50_50);
        case X_2_66_33:
            return build(YUITemplate.YUI_ZT_66_33);
        case X_2_33_66:
            return build(YUITemplate.YUI_ZT_33_66);
        case X_3_DEFAULT:
            return build(YUITemplate.YUI_ZT_33_33_33);
        case X_3_HEADER_2COLS:
            return build(YUITemplate.YUI_ZT_100, YUITemplate.YUI_ZT_50_50);
        case X_4_FOOTER_3COLS:
            return build(YUITemplate.YUI_ZT_33_33_33, YUITemplate.YUI_ZT_100);
        case X_4_HEADER_3COLS:
            return build(YUITemplate.YUI_ZT_100, YUITemplate.YUI_ZT_33_33_33);
        case X_4_66_33_50_50:
            return build(YUITemplate.YUI_ZT_66_33, YUITemplate.YUI_ZT_50_50);
        case X_4_50_50_66_33:
            return build(YUITemplate.YUI_ZT_50_50, YUITemplate.YUI_ZT_66_33);
        case X_4_100_66_33_100:
            return build(YUITemplate.YUI_ZT_100, YUITemplate.YUI_ZT_66_33,
                    YUITemplate.YUI_ZT_100);
        case X_4_100_33_66_100:
            return build(YUITemplate.YUI_ZT_100, YUITemplate.YUI_ZT_33_66,
                    YUITemplate.YUI_ZT_100);
        default:
            return build(YUITemplate.YUI_ZT_100);
        }
    }

    private static YUILayout build(YUITemplate... templates) {
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
