/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Configuration for the digest.
 *
 * @since 11.1
 */
public class DigestConfiguration extends PropertyBasedConfiguration {

    public static final String DIGEST_ALGORITHM_PROPERTY = "digest";

    /** @since 11.5 */
    public static final String DIGEST_ASYNC_PROPERTY = "digestAsync";

    public static final String DEFAULT_DIGEST_ALGORITHM = "MD5";

    public final String digestAlgorithm;

    /** @deprecated since 11.5, use {@link KeyStrategyDigest} instead */
    @Deprecated
    public final Pattern digestPattern;

    /** @since 11.5 */
    public final boolean digestAsync;

    public DigestConfiguration(String digestAlgorithm) {
        super(null, null);
        this.digestAlgorithm = digestAlgorithm;
        digestPattern = getDigestPattern();
        digestAsync = false;
    }

    public DigestConfiguration(String systemPropertyPrefix, Map<String, String> properties) {
        super(systemPropertyPrefix, properties);
        digestAlgorithm = getDigestAlgorithm();
        digestPattern = getDigestPattern();
        digestAsync = getBooleanProperty(DIGEST_ASYNC_PROPERTY);
    }

    protected String getDigestAlgorithm() {
        return getProperty(DIGEST_ALGORITHM_PROPERTY, DEFAULT_DIGEST_ALGORITHM).toUpperCase(Locale.ENGLISH);
    }

    /** @deprecated since 11.5, use {@link KeyStrategyDigest} instead */
    @Deprecated
    protected Pattern getDigestPattern() {
        // compute a dummy digest (from 0-length input) to know its length and derive a regexp
        int len = new DigestUtils(digestAlgorithm).digestAsHex(new byte[0]).length();
        return Pattern.compile("[0-9a-f]{" + len + "}");
    }

    /** @deprecated since 11.5, use {@link KeyStrategyDigest} instead */
    @Deprecated
    public boolean isValidDigest(String digest) {
        return digestPattern.matcher(digest).matches();
    }

}
