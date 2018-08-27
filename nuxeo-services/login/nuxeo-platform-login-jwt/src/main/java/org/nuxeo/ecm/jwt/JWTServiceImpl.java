/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.jwt;

import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The JSON Web Token Service implementation.
 *
 * @since 10.3
 */
public class JWTServiceImpl extends DefaultComponent implements JWTService {

    private static final Log log = LogFactory.getLog(JWTServiceImpl.class);

    public static final String XP_CONFIGURATION = "configuration";

    public static final String NUXEO_ISSUER = "nuxeo";

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected static final TypeReference<Map<String, Object>> MAP_STRING_OBJECT = new TypeReference<Map<String, Object>>() {
    };

    protected static class JWTServiceConfigurationRegistry
            extends SimpleContributionRegistry<JWTServiceConfigurationDescriptor> {

        protected static final String KEY = ""; // value doesn't matter as long as we use a fixed one

        protected static final JWTServiceConfigurationDescriptor DEFAULT_CONTRIBUTION = new JWTServiceConfigurationDescriptor();

        @Override
        public String getContributionId(JWTServiceConfigurationDescriptor contrib) {
            return KEY;
        }

        @Override
        public boolean isSupportingMerge() {
            return true;
        }

        @Override
        public JWTServiceConfigurationDescriptor clone(JWTServiceConfigurationDescriptor orig) {
            return new JWTServiceConfigurationDescriptor(orig);
        }

        @Override
        public void merge(JWTServiceConfigurationDescriptor src, JWTServiceConfigurationDescriptor dst) {
            dst.merge(src);
        }

        public JWTServiceConfigurationDescriptor getContribution() {
            JWTServiceConfigurationDescriptor contribution = getContribution(KEY);
            if (contribution == null) {
                contribution = DEFAULT_CONTRIBUTION;
            }
            return contribution;
        }
    }

    protected final JWTServiceConfigurationRegistry registry = new JWTServiceConfigurationRegistry();

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_CONFIGURATION.equals(extensionPoint)) {
            registry.addContribution((JWTServiceConfigurationDescriptor) contribution);
        } else {
            throw new NuxeoException("Unknown extension point: " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_CONFIGURATION.equals(extensionPoint)) {
            registry.removeContribution((JWTServiceConfigurationDescriptor) contribution);
        }
    }

    // -------------------- JWTService API --------------------

    @Override
    public JWTBuilder newBuilder() {
        return new JWTBuilderImpl();
    }

    /**
     * Implementation of {@link JWTBuilder} delegating to the auth0 JWT library.
     *
     * @since 10.3
     */
    public class JWTBuilderImpl implements JWTBuilder {

        public final Builder builder;

        public JWTBuilderImpl() {
            builder = JWT.create();
            // default Nuxeo issuer, checked during validation
            builder.withIssuer(NUXEO_ISSUER);
            // default to current principal as subject
            String subject = ClientLoginModule.getCurrentPrincipal().getActingUser();
            if (subject == null) {
                throw new NuxeoException("No currently logged-in user");
            }
            builder.withSubject(subject);
            // default TTL
            withTTL(0);
        }

        @Override
        public JWTBuilderImpl withTTL(int ttlSeconds) {
            if (ttlSeconds <= 0) {
                ttlSeconds = getDefaultTTL();
            }
            builder.withExpiresAt(Date.from(Instant.now().plusSeconds(ttlSeconds)));
            return this;
        }

        @Override
        public JWTBuilderImpl withClaim(String name, Object value) {
            if (value instanceof Boolean) {
                builder.withClaim(name, (Boolean) value);
            } else if (value instanceof Date) {
                builder.withClaim(name, (Date) value);
            } else if (value instanceof Double) {
                builder.withClaim(name, (Double) value);
            } else if (value instanceof Integer) {
                builder.withClaim(name, (Integer) value);
            } else if (value instanceof Long) {
                builder.withClaim(name, (Long) value);
            } else if (value instanceof String) {
                builder.withClaim(name, (String) value);
            } else if (value instanceof Integer[]) {
                builder.withArrayClaim(name, (Integer[]) value);
            } else if (value instanceof Long[]) {
                builder.withArrayClaim(name, (Long[]) value);
            } else if (value instanceof String[]) {
                builder.withArrayClaim(name, (String[]) value);
            } else {
                throw new NuxeoException("Unknown claim type: " + value);
            }
            return this;
        }

        @Override
        public String build() {
            try {
                Algorithm algorithm = getAlgorithm();
                if (algorithm == null) {
                    throw new NuxeoException("JWTService secret not configured");
                }
                return builder.sign(algorithm);
            } catch (JWTCreationException e) {
                throw new NuxeoException(e);
            }
        }
    }

    protected void builderWithClaim(Builder builder, String name, Object value) {
        if (value instanceof Boolean) {
            builder.withClaim(name, (Boolean) value);
        } else if (value instanceof Date) {
            builder.withClaim(name, (Date) value);
        } else if (value instanceof Double) {
            builder.withClaim(name, (Double) value);
        } else if (value instanceof Integer) {
            builder.withClaim(name, (Integer) value);
        } else if (value instanceof Long) {
            builder.withClaim(name, (Long) value);
        } else if (value instanceof String) {
            builder.withClaim(name, (String) value);
        } else if (value instanceof Integer[]) {
            builder.withArrayClaim(name, (Integer[]) value);
        } else if (value instanceof Long[]) {
            builder.withArrayClaim(name, (Long[]) value);
        } else if (value instanceof String[]) {
            builder.withArrayClaim(name, (String[]) value);
        } else {
            throw new NuxeoException("Unknown claim type: " + value);
        }
    }

    @Override
    public Map<String, Object> verifyToken(String token) {
        Objects.requireNonNull(token);
        Algorithm algorithm = getAlgorithm();
        if (algorithm == null) {
            log.debug("secret not configured, cannot verify token");
            return null; // no secret
        }
        JWTVerifier verifier = JWT.require(algorithm) //
                                  .withIssuer(NUXEO_ISSUER)
                                  .build();
        DecodedJWT jwt;
        try {
            jwt = verifier.verify(token);
        } catch (JWTVerificationException e) {
            if (log.isTraceEnabled()) {
                log.trace("token verification failed: " + e.toString());
            }
            return null; // invalid
        }
        Object payload = getFieldValue(jwt, "payload"); // com.auth0.jwt.impl.PayloadImpl
        Map<String, JsonNode> tree = getFieldValue(payload, "tree");
        return tree.entrySet().stream().collect(toMap(Entry::getKey, e -> nodeToValue(e.getValue())));
    }

    /**
     * Converts a {@link JsonNode} to a Java value.
     */
    protected static Object nodeToValue(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        } else if (node.isObject()) {
            try {
                try (JsonParser parser = OBJECT_MAPPER.treeAsTokens(node)) {
                    return parser.readValueAs(MAP_STRING_OBJECT);
                }
            } catch (IOException e) {
                throw new NuxeoException("Cannot map claim value to Map", e);
            }
        } else if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode elem : node) {
                try {
                    list.add(OBJECT_MAPPER.treeToValue(elem, Object.class));
                } catch (IOException e) {
                    throw new NuxeoException("Cannot map Claim array value to Object", e);
                }
            }
            return list;
        } else {
            // Jackson doesn't seem to have an easy way to do this, other than checking each possible type
            Object value;
            try {
                value = getFieldValue(node, "_value");
            } catch (NuxeoException e) {
                log.warn("Cannot extract primitive value from JsonNode: " + node.getClass().getName());
                value = null;
            }
            if (value instanceof Integer) {
                // normalize to Long for caller convenience
                value = Long.valueOf(((Integer) value).longValue());
            }
            return value;
        }
    }

    protected int getDefaultTTL() {
        return registry.getContribution().getDefaultTTL();
    }

    protected Algorithm getAlgorithm() {
        String secret = registry.getContribution().getSecret();
        if (isBlank(secret)) {
            return null;
        }
        return Algorithm.HMAC512(secret);
    }

    @SuppressWarnings("unchecked")
    protected static <T> T getFieldValue(Object object, String name) {
        try {
            Field field = object.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(object);
        } catch (ReflectiveOperationException | SecurityException | IllegalArgumentException e) {
            throw new NuxeoException(e);
        }
    }

}
