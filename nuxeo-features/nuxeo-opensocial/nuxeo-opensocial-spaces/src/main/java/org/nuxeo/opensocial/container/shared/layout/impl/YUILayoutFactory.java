package org.nuxeo.opensocial.container.shared.layout.impl;

import java.io.Serializable;

import org.nuxeo.opensocial.container.shared.layout.api.YUILayout;
import org.nuxeo.opensocial.container.shared.layout.enume.YUISideBarStyle;
import org.nuxeo.opensocial.container.shared.layout.enume.YUISize;
import org.nuxeo.opensocial.container.shared.layout.enume.YUITemplate;

/**
 * @author Stéphane Fourrier
 */
public class YUILayoutFactory implements Serializable {
    private static final long serialVersionUID = 1L;

    public static YUILayout createLayout(YUIAbstractBodySize size,
            boolean header, boolean footer, YUISideBarStyle sideBar) {
        return new YUILayoutImpl(size, header, footer, sideBar);
    }

    public static YUILayout createDummies() {
        YUILayout layout = YUILayoutFactory.createLayout(new YUIFixedBodySize(
                YUISize.YUI_BS_FULL_PAGE), true, true,
                YUISideBarStyle.YUI_SB_NO_COLUMN);

        YUIAbstractComponent zone1 = new YUIComponentZoneImpl(
                YUITemplate.YUI_ZT_100);
        layout.getContent().addComponent(zone1);

        ((YUIComponentZoneImpl) zone1).addComponent(new YUIUnitImpl());

        YUIAbstractComponent zone2 = new YUIComponentZoneImpl(
                YUITemplate.YUI_ZT_25_75);
        layout.getContent().addComponent(zone2);

        ((YUIComponentZoneImpl) zone2).addComponent(new YUIUnitImpl());
        ((YUIComponentZoneImpl) zone2).addComponent(new YUIUnitImpl());

        return layout;
    }
}
