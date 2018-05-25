/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.schema.types.resolver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;

/**
 * External references are document field with a simple type whose value refers to an external business entity. Objects
 * implementing this interface are able to resolve the entity using the reference.
 *
 * @since 7.1
 */
public interface ObjectResolver extends Serializable {

    /**
     * @since 10.2
     */
    String VALIDATION_PARAMETER_KEY = "validation";

    /**
     * Configure this resolver.
     *
     * @param parameters A map of parameter whose keys are parameter names and map value are corresponding values.
     * @throws IllegalArgumentException If some parameter are not compatible with this resolver.
     * @throws IllegalStateException If this resolver is already configured.
     * @since 7.1
     */
    void configure(Map<String, String> parameters) throws IllegalArgumentException, IllegalStateException;

    /**
     * Returns the resolved object types.
     *
     * @since 7.2
     */
    List<Class<?>> getManagedClasses();

    /**
     * Provides this resolver name.
     *
     * @return The resolver name.
     * @since 7.1
     */
    String getName();

    /**
     * Provides this resolver parameters.
     *
     * @return A map containing <parameter name , parameter value>
     * @since 7.1
     */
    Map<String, Serializable> getParameters();

    /**
     * Validates some value references an existing entity.
     *
     * @param value The reference.
     * @return true if value could be resolved as an existing external reference, false otherwise.
     * @throws IllegalStateException If this resolver has not been configured.
     * @since 7.1
     */
    boolean validate(Object value);

    /**
     * Provides the entity referenced by a value.
     *
     * @param value The reference.
     * @return The referenced entity, null if no entity matches the value.
     * @throws IllegalStateException If this resolver has not been configured.
     * @since 7.1
     */
    Object fetch(Object value);

    /**
     * Provides the entity referenced by a value, return the entity as expected type.
     *
     * @param value The reference.
     * @return The referenced entity, null if no entity matches the value or if this entity cannot be converted as type.
     * @throws IllegalStateException If this resolver has not been configured.
     * @since 7.1
     */
    <T> T fetch(Class<T> type, Object value);

    /**
     * Generates a reference to an entity.
     *
     * @param object The entity.
     * @return A reference to the entity or null if its not a managed entity type.
     * @throws IllegalStateException If this resolver has not been configured.
     * @since 7.1
     */
    Serializable getReference(Object object);

    /**
     * Provides an error message to display when some invalid value does not match existing entity.
     *
     * @param invalidValue The invalid value that don't match any entity.
     * @param locale The language in which the message should be generated.
     * @return A message in the specified language or
     * @since 7.1
     */
    String getConstraintErrorMessage(Object invalidValue, Locale locale);

    /**
     * Manage translation for resolver : {@link #getConstraintErrorMessage(ObjectResolver, Object, Locale, String...)}
     *
     * @since 7.1
     */
    final class Helper {

        private static final Log log = LogFactory.getLog(Helper.class);

        private Helper() {
        }

        /**
         * Use a default translation key : label.schema.constraint.resolver.[Resolver.getName()]
         *
         * @param resolver The requesting resolver.
         * @param suffixCase This field is a which allow to define alternative translation.
         * @param invalidValue The invalid value that don't match any entity.
         * @param locale The language in which the message should be generated.
         * @param additionnalParameters Relayed elements to build the message.
         * @return A message in the specified language
         * @since 7.1
         */
        public static String getConstraintErrorMessage(ObjectResolver resolver, String suffixCase, Object invalidValue,
                Locale locale, String... additionnalParameters) {
            List<String> pathTokens = new ArrayList<>();
            pathTokens.add(Constraint.MESSAGES_KEY);
            pathTokens.add("resolver");
            pathTokens.add(resolver.getName());
            if (suffixCase != null) {
                pathTokens.add(suffixCase);
            }
            String keyConstraint = StringUtils.join(pathTokens, '.');
            String computedInvalidValue = "null";
            if (invalidValue != null) {
                String invalidValueString = invalidValue.toString();
                if (invalidValueString.length() > 20) {
                    computedInvalidValue = invalidValueString.substring(0, 15) + "...";
                } else {
                    computedInvalidValue = invalidValueString;
                }
            }
            Object[] params = new Object[1 + additionnalParameters.length];
            params[0] = computedInvalidValue;
            System.arraycopy(additionnalParameters, 0, params, 1, params.length - 1);
            Locale computedLocale = locale != null ? locale : Constraint.MESSAGES_DEFAULT_LANG;
            String message;
            try {
                message = I18NUtils.getMessageString(Constraint.MESSAGES_BUNDLE, keyConstraint, params, computedLocale);
            } catch (MissingResourceException e) {
                log.trace("No bundle found", e);
                return null;
            }
            if (message != null && !message.trim().isEmpty() && !keyConstraint.equals(message)) {
                // use a constraint specific message if there's one
                return message;
            } else {
                return String.format("%s cannot resolve reference %s", resolver.getName(), computedInvalidValue);
            }
        }

        /**
         * Use a default translation key : label.schema.constraint.resolver.[Resolver.getName()]
         *
         * @param resolver The requesting resolver.
         * @param invalidValue The invalid value that don't match any entity.
         * @param locale The language in which the message should be generated.
         * @param additionnalParameters Relayed elements to build the message.
         * @return A message in the specified language
         * @since 7.1
         */
        public static String getConstraintErrorMessage(ObjectResolver resolver, Object invalidValue, Locale locale,
                String... additionnalParameters) {
            return Helper.getConstraintErrorMessage(resolver, null, invalidValue, locale, additionnalParameters);
        }
    }

}
