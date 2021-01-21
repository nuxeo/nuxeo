/*
 * (C) Copyright 2012-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     mguillaume
 */

package org.nuxeo.launcher.info;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.nuxeo.launcher.config.ConfigurationGenerator.NUXEO_PROFILES;
import static org.nuxeo.launcher.config.ConfigurationGenerator.TEMPLATE_SEPARATOR;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.nuxeo.common.Environment;
import org.nuxeo.common.codec.Crypto;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.config.ConfigurationHolder;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "instance")
public class InstanceInfo {

    /** @since 11.5 */
    protected static final String DISTRIBUTION_PROPS = "distribution.properties";

    /** @since 11.5 */
    protected static final Path TEMPLATE_DISTRIBUTION_PROPS_PATH = Path.of("common", "config",
            "distribution.properties");

    public InstanceInfo() {
    }

    @XmlElement(name = "NUXEO_CONF")
    public String NUXEO_CONF;

    @XmlElement(name = "NUXEO_HOME")
    public String NUXEO_HOME;

    @XmlElement(name = "clid")
    public String clid;

    @XmlElement(name = "distribution")
    public DistributionInfo distribution;

    @XmlElementWrapper(name = "packages")
    @XmlElement(name = "package")
    public List<PackageInfo> packages = new ArrayList<>();

    @XmlElement(name = "configuration")
    public ConfigurationInfo config;

    /**
     * Introspects the server and builds the instance info.
     *
     * @since 11.5
     */
    public static InstanceInfo from(ConfigurationHolder configHolder, String clid, List<LocalPackage> pkgs) {
        InstanceInfo nxInstance = new InstanceInfo();
        nxInstance.NUXEO_CONF = configHolder.getNuxeoConfPath().toString();
        nxInstance.NUXEO_HOME = configHolder.getHomePath().toString();
        // distribution
        Path distFile = configHolder.getConfigurationPath().resolve(DISTRIBUTION_PROPS);
        if (Files.notExists(distFile)) {
            // fallback in the file in templates
            distFile = configHolder.getTemplatesPath().resolve(TEMPLATE_DISTRIBUTION_PROPS_PATH);
        }
        try {
            nxInstance.distribution = new DistributionInfo(distFile.toFile());
        } catch (IOException e) {
            nxInstance.distribution = new DistributionInfo();
        }
        // packages
        nxInstance.clid = clid;
        Set<String> pkgTemplates = new HashSet<>();
        for (LocalPackage pkg : pkgs) {
            final PackageInfo info = new PackageInfo(pkg);
            nxInstance.packages.add(info);
            pkgTemplates.addAll(info.templates);
        }
        nxInstance.config = new ConfigurationInfo();
        // profiles
        String profiles = System.getenv(NUXEO_PROFILES);
        if (isNotBlank(profiles)) {
            nxInstance.config.profiles.addAll(Arrays.asList(profiles.split(TEMPLATE_SEPARATOR)));
        }
        // templates
        nxInstance.config.dbtemplate = configHolder.getIncludedDBTemplateName();
        List<String> userTemplates = configHolder.getIncludedTemplateNames();
        for (String template : userTemplates) {
            if (template.equals(nxInstance.config.dbtemplate)) {
                continue;
            }
            if (pkgTemplates.contains(template)) {
                nxInstance.config.pkgtemplates.add(template);
            } else {
                if (Files.exists(configHolder.getTemplatesPath().resolve(template))) {
                    nxInstance.config.basetemplates.add(template);
                } else {
                    nxInstance.config.usertemplates.add(template);
                }
            }
        }
        // Settings from nuxeo.conf
        nxInstance.config.keyvals = computeKeyVals(configHolder, configHolder.keySet());
        // Effective configuration for environment and profiles
        nxInstance.config.allkeyvals = computeKeyVals(configHolder, configHolder.stringPropertyNames());
        return nxInstance;
    }

    protected static List<KeyValueInfo> computeKeyVals(ConfigurationHolder configHolder, Set<String> keys) {
        var keyVals = new ArrayList<KeyValueInfo>(keys.size());
        for (String key : new TreeSet<>(keys)) {
            String value = configHolder.getRawProperty(key);
            if (ConfigurationGenerator.SECRET_KEYS.contains(key) || key.contains("password")
                    || key.equals(Environment.SERVER_STATUS_KEY) || Crypto.isEncrypted(value)) {
                value = "********";
            }
            keyVals.add(new KeyValueInfo(key, value));
        }
        return keyVals;
    }
}
