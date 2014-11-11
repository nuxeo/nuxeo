package org.nuxeo.opensocial.container.client.view;

public class I18nUtils {
    private static final String __MSG_END = "__";
    private static final String __MSG_ = "__MSG_";


    static String getI18nKey(String dispName) {
        return dispName.substring(__MSG_.length(),
                dispName.length() - __MSG_END.length());
    }

    static boolean isI18nLabel(String dispName) {
        return dispName.startsWith(__MSG_) && dispName.endsWith(__MSG_END);
    }

}
