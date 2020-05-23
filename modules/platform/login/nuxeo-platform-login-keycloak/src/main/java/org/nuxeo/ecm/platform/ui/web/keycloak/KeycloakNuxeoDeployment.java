/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     François Maturel
 */

package org.nuxeo.ecm.platform.ui.web.keycloak;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.util.SystemPropertiesJsonParserFactory;

/**
 * This class is developed to overcome a Jackson version problem between Nuxeo and Keycloak.<br>
 * Nuxeo uses Jackson version 1.8.x where Keycloak uses 1.9.x<br>
 * Sadly the {@link ObjectMapper#setSerializationInclusion} method is not in 1.8.x<br>
 * Then {@link KeycloakNuxeoDeployment} is the same class as {@link KeycloakDeploymentBuilder}, rewriting static method
 * {@link KeycloakDeploymentBuilder#loadAdapterConfig} to avoid the use of
 * {@link ObjectMapper#setSerializationInclusion}
 *
 * @since 7.4
 */
public class KeycloakNuxeoDeployment {

    /**
     * Invokes KeycloakDeploymentBuilder.internalBuild with reflection to avoid rewriting source code
     *
     * @param is the configuration file {@link InputStream}
     * @return the {@link KeycloakDeployment} corresponding to the configuration file
     */
    public static KeycloakDeployment build(InputStream is) {
        AdapterConfig adapterConfig = loadAdapterConfig(is);

        try {
            Constructor<KeycloakDeploymentBuilder> constructor = KeycloakDeploymentBuilder.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            KeycloakDeploymentBuilder builder = constructor.newInstance();
            return (KeycloakDeployment) MethodUtils.invokeMethod(builder, true, "internalBuild", adapterConfig);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static AdapterConfig loadAdapterConfig(InputStream is) {
        ObjectMapper mapper = new ObjectMapper(new SystemPropertiesJsonParserFactory());
        AdapterConfig adapterConfig;
        try {
            adapterConfig = mapper.readValue(is, AdapterConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return adapterConfig;
    }

}
