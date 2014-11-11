/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.opensocial.shindig.oauth;

import net.oauth.OAuthConsumer;
import net.oauth.OAuthProblemException;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.social.opensocial.oauth.OAuthDataStore;
import org.apache.shindig.social.opensocial.oauth.OAuthEntry;

import com.google.inject.Singleton;

@Singleton
public class NXOAuthDataStore implements OAuthDataStore {

    public void authorizeToken(OAuthEntry arg0, String arg1)
            throws OAuthProblemException {
        // TODO Auto-generated method stub

    }

    public OAuthEntry convertToAccessToken(OAuthEntry arg0)
            throws OAuthProblemException {
        // TODO Auto-generated method stub
        return null;
    }

    public void disableToken(OAuthEntry arg0) {
        // TODO Auto-generated method stub

    }

    public OAuthEntry generateRequestToken(String arg0, String arg1, String arg2)
            throws OAuthProblemException {
        // TODO Auto-generated method stub
        return null;
    }

    public OAuthConsumer getConsumer(String arg0) throws OAuthProblemException {
        // TODO Auto-generated method stub
        return null;
    }

    public OAuthEntry getEntry(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public SecurityToken getSecurityTokenForConsumerRequest(String arg0,
            String arg1) throws OAuthProblemException {
        // TODO Auto-generated method stub
        return null;
    }

    public void removeToken(OAuthEntry arg0) {
        // TODO Auto-generated method stub

    }

}
