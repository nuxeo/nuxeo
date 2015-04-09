package org.nuxeo.ecm.webengine.gwt;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;

@XObject("applications")
public class GwtAppDescriptor {

    @XNode("@name")
    public String name;

    GwtAppResolver resolver = GwtResolver.ROOT_RESOLVER;

    @XNode("resolver")
    void setResolver(Class<? extends GwtAppResolver> type) {
        try {
            resolver = type.newInstance();
        } catch (ReflectiveOperationException  cause) {
            throw new NuxeoException("Cannot load " + type, cause);
        }
    }

}
