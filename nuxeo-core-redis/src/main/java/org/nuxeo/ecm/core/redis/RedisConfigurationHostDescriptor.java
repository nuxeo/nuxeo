package org.nuxeo.ecm.core.redis;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("host")
public class RedisConfigurationHostDescriptor {

    @XNode("@name")
    public String name;

    @XNode("@port")
    public int port;

    protected RedisConfigurationHostDescriptor(String name, int port) {
        this.name = name;
        this.port = port;
    }
}
