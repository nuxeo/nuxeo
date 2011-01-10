package org.nuxeo.ecm.platform.oauth.consumers;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

import net.oauth.OAuthConsumer;
import net.oauth.OAuthServiceProvider;

public class NuxeoOAuthConsumer extends OAuthConsumer {

    private static final long serialVersionUID = 1L;

    public static final String ALLOW_SIGNEDFETCH= "allowSignedFetch";

    public static final String DESCRIPTION= "description";

    public static final String ENABLED= "enabled";

    protected static final String SCHEMA = "oauthConsumer";

    public static NuxeoOAuthConsumer createFromDirectoryEntry (DocumentModel entry) throws ClientException {
        String callbackURL = (String)entry.getProperty(SCHEMA, "callbackURL");
        String consumerKey = (String)entry.getProperty(SCHEMA, "consumerKey");
        String consumerSecret = (String)entry.getProperty(SCHEMA, "consumerSecret");
        // XXX
        return new NuxeoOAuthConsumer(callbackURL, consumerKey, consumerSecret, null);
    }

    public NuxeoOAuthConsumer(String callbackURL, String consumerKey,
            String consumerSecret, OAuthServiceProvider serviceProvider) {
        super(callbackURL, consumerKey, consumerSecret, serviceProvider);
    }

    protected DocumentModel asDocumentModel(DocumentModel entry) throws ClientException {
        entry.setProperty(SCHEMA, "callbackURL", callbackURL);
        entry.setProperty(SCHEMA, "consumerKey", consumerKey);
        entry.setProperty(SCHEMA, "consumerSecret", consumerSecret);
        // XXX

        return entry;
    }

    public boolean allowSignedFetch() {
        Object prop = getProperty(ALLOW_SIGNEDFETCH);
        if (prop==null) {
            return false;
        } else {
            return (Boolean) prop;
        }
    }

    public String getDescription() {
        Object prop = getProperty(DESCRIPTION);
        if (prop==null) {
            return null;
        } else {
            return (String) prop;
        }
    }




}
