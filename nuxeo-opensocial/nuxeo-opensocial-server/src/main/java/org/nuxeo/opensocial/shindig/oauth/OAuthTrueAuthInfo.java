package org.nuxeo.opensocial.shindig.oauth;

import java.util.HashMap;
import java.util.Map;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.spec.RequestAuthenticationInfo;

public class OAuthTrueAuthInfo implements RequestAuthenticationInfo {

    protected Uri uri;

    protected HashMap<String, String> attributes;

    public OAuthTrueAuthInfo(Uri uri, String ownerId, String viewerId,
            String service) {
        this.uri = uri;
        attributes = new HashMap<String, String>();
        // these are not constants in the OAuthFetcher, who consumes them!
        attributes.put("owner", ownerId);
        attributes.put("viewer", viewerId);

        // not visible to us
        attributes.put("OAUTH_SERVICE_NAME", service);

    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public AuthType getAuthType() {
        return AuthType.OAUTH;
    }

    public Uri getHref() {
        return uri;
    }

    public boolean isSignOwner() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isSignViewer() {
        // TODO Auto-generated method stub
        return false;
    }

}