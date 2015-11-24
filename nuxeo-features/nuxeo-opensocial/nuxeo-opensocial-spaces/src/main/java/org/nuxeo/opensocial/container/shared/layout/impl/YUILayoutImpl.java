package org.nuxeo.opensocial.container.shared.layout.impl;

import java.io.Serializable;

import org.nuxeo.opensocial.container.shared.layout.api.YUIBodySize;
import org.nuxeo.opensocial.container.shared.layout.api.YUIComponent;
import org.nuxeo.opensocial.container.shared.layout.api.YUIContent;
import org.nuxeo.opensocial.container.shared.layout.api.YUILayout;
import org.nuxeo.opensocial.container.shared.layout.api.YUIUnit;
import org.nuxeo.opensocial.container.shared.layout.enume.YUISideBarStyle;
import org.nuxeo.opensocial.container.shared.layout.enume.YUISize;

/**
 * @author Stéphane Fourrier
 */
public class YUILayoutImpl implements YUILayout, Serializable {
    private static final long serialVersionUID = 1L;

    private YUIBodySize size;

    private YUISideBarStyle sidebarStyle;

    // TODO use YUIContent instead
    private YUIContentImpl content;

    private YUIUnit header;

    private YUIUnit footer;

    private YUIUnit sidebar;

    public YUILayoutImpl() {
        setBodySize(new YUIFixedBodySize(YUISize.YUI_BS_FULL_PAGE));
        setSideBarStyle(YUISideBarStyle.YUI_SB_NO_COLUMN);
        setHeader(null);
        setContent(new YUIContentImpl());
        setFooter(null);
    }

    public YUILayoutImpl(YUIAbstractBodySize size, boolean hasHeader,
            boolean hasFooter, YUISideBarStyle sideBar) {
        setBodySize(size);
        setSideBarStyle(sideBar);
        if (hasHeader) {
            setHeader(new YUIUnitImpl());
        }
        setContent(new YUIContentImpl());
        if(hasFooter) {
            setFooter(new YUIUnitImpl());
        }
    }

    public YUISideBarStyle getSidebarStyle() {
        return sidebarStyle;
    }

    public void setSideBarStyle(YUISideBarStyle sideBar) {
        this.sidebarStyle = sideBar;
    }

    public YUIUnit getSideBar() {
        return sidebar;
    }

    public void setSideBar(YUIUnit sidebar) {
        this.sidebar = sidebar;
    }

    public void setBodySize(YUIBodySize size) {
        this.size = size;
    }

    public YUIBodySize getBodySize() {
        return this.size;
    }

    public YUIContentImpl getContent() {
        return content;
    }

    public void setContent(YUIContent content) {
        this.content = (YUIContentImpl) content;
    }

    public void setHeader(YUIUnit header) {
        this.header = header;
    }

    public YUIUnit getHeader() {
        return header;
    }

    public void setFooter(YUIUnit footer) {
        this.footer = footer;
    }

    public YUIUnit getFooter() {
        return footer;
    }

    public void copyFrom(YUILayout layout) {
        setBodySize(layout.getBodySize());
        setFooter(layout.getFooter());
        setHeader(layout.getHeader());
        setSideBarStyle(layout.getSidebarStyle());

        setContent(new YUIContentImpl());
        YUIContentImpl content = getContent();
        for (YUIComponent component : layout.getContent().getComponents()) {
            content.addComponent((YUIComponent) component.getACopyFor());
        }
    }
}
