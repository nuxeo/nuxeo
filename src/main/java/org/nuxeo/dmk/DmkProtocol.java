package org.nuxeo.dmk;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;

@XObject("protocol")
public class DmkProtocol {

    @XNode("@name")
    public String name = "html";

    @XNode("port")
    public int port = 8081;

    @XNode("user")
    public String user = "operator";

    @XNode("password")
    public String password = Framework.getProperty("server.status.key", "pfouh");

    @Override
    public String toString() {
        return "DmkProtocol [name=" + name + ", port=" + port + ", user="
                + user + "]";
    }

}
