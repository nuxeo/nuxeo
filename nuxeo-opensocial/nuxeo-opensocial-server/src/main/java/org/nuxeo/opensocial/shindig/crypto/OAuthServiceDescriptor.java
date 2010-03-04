package org.nuxeo.opensocial.shindig.crypto;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("oauthservice")
public class OAuthServiceDescriptor {

    @XNode("gadgetUrl")
    protected String gadgetUrl;

    @XNode("serviceName")
    protected String serviceName;

    @XNode("consumerKey")
    protected String consumerKey;

    @XNode("consumerSecret")
    protected String consumerSecret;

    public String getGadgetUrl() {
        return gadgetUrl;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

}
