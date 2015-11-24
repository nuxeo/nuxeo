package org.nuxeo.opensocial.container.shared.layout.api;

import java.io.Serializable;

import org.nuxeo.opensocial.container.shared.layout.enume.YUISideBarStyle;

/**
 * @author Stéphane Fourrier
 */
public interface YUILayout extends Serializable {
    YUISideBarStyle getSidebarStyle();

    void setSideBarStyle(YUISideBarStyle sideBar);

    YUIUnit getSideBar();

    void setSideBar(YUIUnit sidebar);

    void setBodySize(YUIBodySize size);

    YUIBodySize getBodySize();

    void setHeader(YUIUnit header);

    YUIUnit getHeader();

    void setFooter(YUIUnit footer);

    YUIUnit getFooter();

    void setContent(YUIContent content);

    YUIContent getContent();

    void copyFrom(YUILayout layout);
}
