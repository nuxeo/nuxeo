package org.nuxeo.opensocial.container.client.utils;

import com.google.gwt.user.client.Element;

public class ElementUtils {
    // This method is a way to remove all class style (by giving a class prefix)
    // from an element.
    public static void removeStyle(Element element, String stylePrefix) {
        String temp = element.getClassName();
        for (String classStyle : temp.split(" ")) {
            if (classStyle.startsWith(stylePrefix)) {
                temp = temp.replace(classStyle, "");
            }
        }
        element.setClassName(temp);
    }
}
