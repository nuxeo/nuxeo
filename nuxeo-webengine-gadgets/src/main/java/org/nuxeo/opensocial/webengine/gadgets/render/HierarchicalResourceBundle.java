package org.nuxeo.opensocial.webengine.gadgets.render;

import java.io.IOException;
import java.io.InputStream;
import java.util.PropertyResourceBundle;

public class HierarchicalResourceBundle extends PropertyResourceBundle {

    protected static PropertyResourceBundle commonBundle;

    public HierarchicalResourceBundle(InputStream stream) throws IOException {
        super(stream);
        this.parent = getCommonBundle();
    }

    protected static PropertyResourceBundle getCommonBundle()
            throws IOException {
        if (commonBundle == null) {
            commonBundle = new PropertyResourceBundle(
                    HierarchicalResourceBundle.class.getClassLoader().getResourceAsStream(
                            "skin/resources/i18n/common_messages.properties"));
        }
        return commonBundle;
    }

}
