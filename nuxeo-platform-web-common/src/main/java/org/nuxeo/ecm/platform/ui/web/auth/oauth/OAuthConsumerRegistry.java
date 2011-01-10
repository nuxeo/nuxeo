package org.nuxeo.ecm.platform.ui.web.auth.oauth;

import net.oauth.OAuthConsumer;

public class OAuthConsumerRegistry {

    public OAuthConsumer getConsumer(String consumerKey) {
        if (consumerKey.equals("confluence")) {
            OAuthConsumer consumer = new OAuthConsumer(null,consumerKey,"testoauthsharedsecret", null );
            return consumer;
        }
        return null;
    }

}
