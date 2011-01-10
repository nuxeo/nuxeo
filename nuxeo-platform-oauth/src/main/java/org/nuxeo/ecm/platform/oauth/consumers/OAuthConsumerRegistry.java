package org.nuxeo.ecm.platform.oauth.consumers;

import java.util.List;


public interface OAuthConsumerRegistry {

    NuxeoOAuthConsumer getConsumer(String consumerKey);

    void deleteConsumer(String consumerKey);

    List<NuxeoOAuthConsumer> listConsumers();

    NuxeoOAuthConsumer storeConsumer(NuxeoOAuthConsumer consumer) throws Exception;
}
