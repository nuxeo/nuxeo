package org.nuxeo.ecm.platform.api.ws;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.7.2
 */
@XObject("endpoint")
public class WSEndpointDescriptor {
    @XNode("@name")
    public String name;

    @XNode("@address")
    public String address;

    @XNode("@implementor")
    public Class<?> clazz;

    public Object getImplementorInstance() {
        try {
            return clazz != null ? clazz.newInstance() : null;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Cannot instantiate " + clazz.getName());
        }
    }
}
