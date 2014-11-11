/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.ecm.platform.oauth.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.OAuthServiceProvider;
import net.oauth.signature.OAuthSignatureMethod;
import net.oauth.signature.RSA_SHA1;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.oauth.api.OAuthKeyDescriptor;
import org.nuxeo.ecm.platform.oauth.api.OAuthService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class OAuthServiceImpl extends DefaultComponent implements
    OAuthService {

  private Map<String, OAuthKeyDescriptor> keys = new HashMap<String, OAuthKeyDescriptor>();
  private static final String XP_KEYS = "oauthKey";

  private static final Log log = LogFactory.getLog(OAuthServiceImpl.class);

  @Override
  public void activate(ComponentContext context) throws Exception {
    log.debug("Activating component : " + OAuthServiceImpl.class);
  }

  @Override
  public void deactivate(ComponentContext context) throws Exception {
    log.debug("Deactivating component : " + OAuthServiceImpl.class);
  }

  @Override
  public void registerContribution(Object contribution, String extensionPoint,
      ComponentInstance contributor) throws Exception {
    if (XP_KEYS.equals(extensionPoint)) {
      OAuthKeyDescriptor keydesc = (OAuthKeyDescriptor) contribution;
      log.info("Registering key for consumer : " + keydesc.consumer);
      keys.put(keydesc.consumer, keydesc);
    }
  }

  @Override
  public void unregisterContribution(Object contribution,
      String extensionPoint, ComponentInstance contributor) throws Exception {
    if (XP_KEYS.equals(extensionPoint)) {
      OAuthKeyDescriptor keydesc = (OAuthKeyDescriptor) contribution;
      log.info("UnRegistering key for consumer : " + keydesc.consumer);
      keys.remove(keydesc.consumer);
    }
  }

  public OAuthConsumer getOAuthConsumer(String consumerKey) {
    OAuthKeyDescriptor desc = keys.get(consumerKey);

    //Signed OAuthProvider (no 3 way)
    OAuthServiceProvider provider = new OAuthServiceProvider(null, null,
        null);

    //Returns a consumer for signed request (no callBack Url, no consumer secret)
    OAuthConsumer consumer = new OAuthConsumer(null, consumerKey,
        null, provider);

    if(!"".equals(desc.publicKey)) {
      consumer.setProperty(RSA_SHA1.PUBLIC_KEY , desc.publicKey);
    }

    if(!"".equals(desc.privateKey)) {
      consumer.setProperty(RSA_SHA1.PRIVATE_KEY, desc.privateKey);
    }

    if(!"".equals(desc.certificate)) {
      consumer.setProperty(RSA_SHA1.X509_CERTIFICATE, desc.certificate);
    }

    return consumer;

  }

  public boolean verify(OAuthMessage message, String consumerKey) {
    // TODO Auto-generated method stub
    OAuthAccessor serverAccessor = new OAuthAccessor(this.getOAuthConsumer(consumerKey));
    try {
      OAuthSignatureMethod.newSigner(message, serverAccessor)
          .validate(message);
      return true;
    } catch (OAuthProblemException e) {
      logException(message, e);
    } catch (OAuthException e) {
      log.error(e.getMessage(), e);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    } catch (URISyntaxException e) {
      log.error(e.getMessage(), e);
    }
    return false;
  }

  private void logException(OAuthMessage message, OAuthProblemException e) {
    log.error(e.getProblem(), e);
    try {
      log.debug("message.signature=[" + message.getSignature() + "]");
    } catch (IOException e1) {
      log.error("message.signature throws IOException", e1);
    }
    try {
      log.debug("message.consumerKey=[" + message.getConsumerKey() + "]");
    } catch (IOException e1) {
      log.error("message.consumerKey throws IOException", e1);
    }
    try {
      log.debug("message.signatureMethod=[" + message.getSignatureMethod()
          + "]");
    } catch (IOException e1) {
      log.debug("message.signatureMethod throws IOException", e1);
    }
    try {
      log.debug("message.token=[" + message.getToken() + "]");
    } catch (IOException e1) {
      log.debug("message.token throws IOException", e1);
    }

    Map<String, Object> params = e.getParameters();
    if (params != null)
      for (String key : params.keySet()) {
        Object value = params.get(key);
        log.info("parameter " + key + "=" + value + ".");
      }
  }



  @Override
  @SuppressWarnings("unchecked")
  public <T> T getAdapter(Class<T> adapter) {
      if (adapter.isAssignableFrom(OAuthService.class)) {
          return (T) this;
      }
      return null;
  }







}
