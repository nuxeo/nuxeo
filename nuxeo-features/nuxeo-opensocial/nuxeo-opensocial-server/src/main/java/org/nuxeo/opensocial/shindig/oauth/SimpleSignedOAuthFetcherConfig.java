/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.nuxeo.opensocial.shindig.oauth;

import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.util.TimeSource;
import org.apache.shindig.gadgets.http.HttpCache;
import org.apache.shindig.gadgets.oauth.OAuthFetcherConfig;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Configuration parameters for an OAuthFetcher
 */
public class SimpleSignedOAuthFetcherConfig extends OAuthFetcherConfig {

  @Inject
  public SimpleSignedOAuthFetcherConfig(
      @Named(OAUTH_STATE_CRYPTER) BlobCrypter stateCrypter,
      SignedFetchGadgetOAuthTokenStore tokenStore,
      HttpCache httpCache,
      TimeSource clock) {
    super(stateCrypter, tokenStore, httpCache, clock);
  }

}
