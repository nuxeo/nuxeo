/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.core.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.targetplatforms.api.TargetPackage;
import org.nuxeo.targetplatforms.api.TargetPackageInfo;
import org.nuxeo.targetplatforms.api.TargetPlatform;
import org.nuxeo.targetplatforms.api.TargetPlatformFilter;
import org.nuxeo.targetplatforms.api.TargetPlatformInfo;
import org.nuxeo.targetplatforms.api.TargetPlatformInstance;
import org.nuxeo.targetplatforms.api.impl.TargetPackageImpl;
import org.nuxeo.targetplatforms.api.impl.TargetPackageInfoImpl;
import org.nuxeo.targetplatforms.api.impl.TargetPlatformFilterImpl;
import org.nuxeo.targetplatforms.api.impl.TargetPlatformImpl;
import org.nuxeo.targetplatforms.api.impl.TargetPlatformInfoImpl;
import org.nuxeo.targetplatforms.api.impl.TargetPlatformInstanceImpl;
import org.nuxeo.targetplatforms.api.service.TargetPlatformService;
import org.nuxeo.targetplatforms.core.descriptors.ServiceConfigurationDescriptor;
import org.nuxeo.targetplatforms.core.descriptors.TargetPackageDescriptor;
import org.nuxeo.targetplatforms.core.descriptors.TargetPlatformDescriptor;

/**
 * {@link TargetPlatformService} implementation relying on runtime extension points.
 *
 * @since 5.7.1
 */
public class TargetPlatformServiceImpl extends DefaultComponent implements TargetPlatformService {

    private static final Log log = LogFactory.getLog(TargetPlatformServiceImpl.class);

    public static final String XP_CONF = "configuration";

    public static final String XP_PLATFORMS = "platforms";

    public static final String XP_PACKAGES = "packages";

    protected static final DateTimeFormatter dateParser = DateTimeFormat.forPattern("yyyy/MM/dd").withLocale(
            Locale.ENGLISH).withZone(DateTimeZone.UTC);

    protected ServiceConfigurationRegistry conf;

    protected TargetPlatformRegistry platforms;

    protected TargetPackageRegistry packages;

    // Runtime component API

    @Override
    public void activate(ComponentContext context) {
        platforms = new TargetPlatformRegistry();
        packages = new TargetPackageRegistry();
        conf = new ServiceConfigurationRegistry();
    }

    @Override
    public void deactivate(ComponentContext context) {
        platforms = null;
        packages = null;
        conf = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_PLATFORMS.equals(extensionPoint)) {
            TargetPlatformDescriptor desc = (TargetPlatformDescriptor) contribution;
            log.info(String.format("Register target platform '%s'", desc.getId()));
            platforms.addContribution(desc);
        } else if (XP_PACKAGES.equals(extensionPoint)) {
            TargetPackageDescriptor desc = (TargetPackageDescriptor) contribution;
            log.info(String.format("Register target package '%s'", desc.getId()));
            packages.addContribution(desc);
        } else if (XP_CONF.equals(extensionPoint)) {
            ServiceConfigurationDescriptor desc = (ServiceConfigurationDescriptor) contribution;
            log.info(String.format("Register TargetPlatformService configuration"));
            conf.addContribution(desc);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_PLATFORMS.equals(extensionPoint)) {
            TargetPlatformDescriptor desc = (TargetPlatformDescriptor) contribution;
            log.info(String.format("Unregister target platform '%s'", desc.getId()));
            platforms.removeContribution(desc);
        } else if (XP_PACKAGES.equals(extensionPoint)) {
            TargetPackageDescriptor desc = (TargetPackageDescriptor) contribution;
            log.info(String.format("Unregister target package '%s'", desc.getId()));
            packages.removeContribution(desc);
        } else if (XP_CONF.equals(extensionPoint)) {
            ServiceConfigurationDescriptor desc = (ServiceConfigurationDescriptor) contribution;
            log.info(String.format("Unregister TargetPlatformService configuration"));
            conf.removeContribution(desc);
        }
    }

    // Service API

    @Override
    public TargetPlatform getDefaultTargetPlatform(TargetPlatformFilter filter) {
        List<TargetPlatform> tps = getAvailableTargetPlatforms(filter);
        if (tps.isEmpty()) {
            return null;
        }
        TargetPlatform defaultTP = null;
        for (TargetPlatform tp : tps) {
            if (tp.isDefault()) {
                if (!tp.isRestricted()) {
                    // Return the first default and unrestricted target platform
                    return tp;
                }
                // If the target platform is restricted, we keep it in case no
                // unrestricted target platform is found
                if (defaultTP == null) {
                    defaultTP = tp;
                }
            }
        }
        return defaultTP;
    }

    @Override
    public String getOverrideDirectory() {
        String res = DirectoryUpdater.DEFAULT_DIR;
        ServiceConfigurationDescriptor desc = conf.getConfiguration();
        if (desc == null) {
            return res;
        }
        String id = desc.getOverrideDirectory();
        if (!StringUtils.isBlank(id)) {
            res = id;
        }
        return res;
    }

    @Override
    public TargetPlatform getTargetPlatform(String id) {
        if (id == null) {
            return null;
        }
        TargetPlatformDescriptor desc = platforms.getTargetPlatform(id);
        return getTargetPlatform(desc);
    }

    protected TargetPlatform getTargetPlatform(TargetPlatformDescriptor desc) {
        if (desc == null) {
            return null;
        }
        String id = desc.getId();
        TargetPlatformImpl tp = new TargetPlatformImpl(id, desc.getName(), desc.getVersion(), desc.getRefVersion(),
                desc.getLabel());
        tp.setDeprecated(desc.isDeprecated());
        tp.setDescription(desc.getDescription());
        tp.setDownloadLink(desc.getDownloadLink());
        tp.setEnabled(desc.isEnabled());
        tp.setEndOfAvailability(toDate(desc.getEndOfAvailability()));
        tp.setFastTrack(desc.isFastTrack());
        tp.setTrial(desc.isTrial());
        tp.setDefault(desc.isDefault());
        tp.setParent(getTargetPlatform(desc.getParent()));
        tp.setRefVersion(desc.getRefVersion());
        tp.setReleaseDate(toDate(desc.getReleaseDate()));
        tp.setRestricted(desc.isRestricted());
        tp.setStatus(desc.getStatus());
        tp.setTestVersions(desc.getTestVersions());
        tp.setTypes(desc.getTypes());
        // resolve available packages
        tp.setAvailablePackages(getTargetPackages(id));

        // check if there's an override
        DocumentModel entry = getDirectoryEntry(id);
        if (entry != null) {
            Long enabled = (Long) entry.getProperty(DirectoryUpdater.SCHEMA, DirectoryUpdater.ENABLED_PROP);
            if (enabled != null && enabled.intValue() >= 0) {
                tp.setEnabled(enabled.intValue() == 0 ? false : true);
            }
            Long restricted = (Long) entry.getProperty(DirectoryUpdater.SCHEMA, DirectoryUpdater.RESTRICTED_PROP);
            if (restricted != null && restricted.intValue() >= 0) {
                tp.setRestricted(restricted.intValue() == 0 ? false : true);
            }
            Long deprecated = (Long) entry.getProperty(DirectoryUpdater.SCHEMA, DirectoryUpdater.DEPRECATED_PROP);
            if (deprecated != null && deprecated.intValue() >= 0) {
                tp.setDeprecated(deprecated.intValue() == 0 ? false : true);
            }
            Long trial = (Long) entry.getProperty(DirectoryUpdater.SCHEMA, DirectoryUpdater.TRIAL_PROP);
            if (trial != null && trial.intValue() >= 0) {
                tp.setTrial(trial.intValue() == 0 ? false : true);
            }
            Long isDefault = (Long) entry.getProperty(DirectoryUpdater.SCHEMA, DirectoryUpdater.DEFAULT_PROP);
            if (isDefault != null && isDefault.intValue() >= 0) {
                tp.setDefault(isDefault.intValue() == 0 ? false : true);
            }
            tp.setOverridden(true);
        }

        return tp;
    }

    /**
     * Lookup all packages referencing this target platform.
     */
    protected Map<String, TargetPackage> getTargetPackages(String targetPlatform) {
        Map<String, TargetPackage> tps = new HashMap<>();
        List<TargetPackageDescriptor> pkgs = packages.getTargetPackages(targetPlatform);
        if (pkgs != null) {
            for (TargetPackageDescriptor pkg : pkgs) {
                TargetPackage tp = getTargetPackage(pkg);
                if (tp != null) {
                    tps.put(tp.getId(), tp);
                }
            }
        }
        return tps;
    }

    protected Map<String, TargetPackageInfo> getTargetPackagesInfo(String targetPlatform) {
        Map<String, TargetPackageInfo> tps = new HashMap<>();
        List<TargetPackageDescriptor> pkgs = packages.getTargetPackages(targetPlatform);
        if (pkgs != null) {
            for (TargetPackageDescriptor pkg : pkgs) {
                TargetPackageInfo tp = getTargetPackageInfo(pkg.getId());
                if (tp != null) {
                    tps.put(tp.getId(), tp);
                }
            }
        }
        return tps;
    }

    protected Date toDate(String date) {
        if (StringUtils.isBlank(date)) {
            return null;
        }
        DateTime dt = dateParser.parseDateTime(date);
        return dt.toDate();
    }

    @Override
    public TargetPlatformInfo getTargetPlatformInfo(String id) {
        if (id == null) {
            return null;
        }
        TargetPlatformDescriptor desc = platforms.getTargetPlatform(id);
        TargetPlatformInfo tpi = getTargetPlatformInfo(desc);
        return tpi;
    }

    protected TargetPlatformInfo getTargetPlatformInfo(TargetPlatformDescriptor desc) {
        if (desc == null) {
            return null;
        }
        String id = desc.getId();
        TargetPlatformInfoImpl tpi = new TargetPlatformInfoImpl(id, desc.getName(), desc.getVersion(),
                desc.getRefVersion(), desc.getLabel());
        tpi.setDescription(desc.getDescription());
        tpi.setStatus(desc.getStatus());
        tpi.setEnabled(desc.isEnabled());
        tpi.setFastTrack(desc.isFastTrack());
        tpi.setReleaseDate(toDate(desc.getReleaseDate()));
        tpi.setRestricted(desc.isRestricted());
        tpi.setEndOfAvailability(toDate(desc.getEndOfAvailability()));
        tpi.setDownloadLink(desc.getDownloadLink());
        tpi.setDeprecated(desc.isDeprecated());
        tpi.setAvailablePackagesInfo(getTargetPackagesInfo(id));
        tpi.setTypes(desc.getTypes());
        tpi.setTrial(desc.isTrial());
        tpi.setDefault(desc.isDefault());

        DocumentModel entry = getDirectoryEntry(id);
        if (entry != null) {
            Long enabled = (Long) entry.getProperty(DirectoryUpdater.SCHEMA, DirectoryUpdater.ENABLED_PROP);
            if (enabled != null && enabled.intValue() >= 0) {
                tpi.setEnabled(enabled.intValue() == 0 ? false : true);
            }
            Long restricted = (Long) entry.getProperty(DirectoryUpdater.SCHEMA, DirectoryUpdater.RESTRICTED_PROP);
            if (restricted != null && restricted.intValue() >= 0) {
                tpi.setRestricted(restricted.intValue() == 0 ? false : true);
            }
            Long deprecated = (Long) entry.getProperty(DirectoryUpdater.SCHEMA, DirectoryUpdater.DEPRECATED_PROP);
            if (deprecated != null && deprecated.intValue() >= 0) {
                tpi.setDeprecated(deprecated.intValue() == 0 ? false : true);
            }
            Long trial = (Long) entry.getProperty(DirectoryUpdater.SCHEMA, DirectoryUpdater.TRIAL_PROP);
            if (trial != null && trial.intValue() >= 0) {
                tpi.setTrial(trial.intValue() == 0 ? false : true);
            }
            Long isDefault = (Long) entry.getProperty(DirectoryUpdater.SCHEMA, DirectoryUpdater.DEFAULT_PROP);
            if (isDefault != null && isDefault.intValue() >= 0) {
                tpi.setDefault(isDefault.intValue() == 0 ? false : true);
            }
            tpi.setOverridden(true);
        }

        return tpi;
    }

    @Override
    public TargetPackage getTargetPackage(String id) {
        if (id == null) {
            return null;
        }
        return getTargetPackage(packages.getTargetPackage(id));
    }

    @Override
    public TargetPackageInfo getTargetPackageInfo(String id) {
        if (id == null) {
            return null;
        }
        TargetPackageDescriptor desc = packages.getTargetPackage(id);
        TargetPackageInfoImpl tpi = new TargetPackageInfoImpl(desc.getId(), desc.getName(), desc.getVersion(),
                desc.getRefVersion(), desc.getLabel());
        tpi.setDescription(desc.getDescription());
        tpi.setStatus(desc.getStatus());
        tpi.setEnabled(desc.isEnabled());
        tpi.setReleaseDate(toDate(desc.getReleaseDate()));
        tpi.setRestricted(desc.isRestricted());
        tpi.setEndOfAvailability(toDate(desc.getEndOfAvailability()));
        tpi.setDownloadLink(desc.getDownloadLink());
        tpi.setDeprecated(desc.isDeprecated());
        tpi.setDependencies(desc.getDependencies());
        return tpi;
    }

    protected TargetPackage getTargetPackage(TargetPackageDescriptor desc) {
        if (desc == null) {
            return null;
        }
        TargetPackageImpl tp = new TargetPackageImpl(desc.getId(), desc.getName(), desc.getVersion(),
                desc.getRefVersion(), desc.getLabel());
        tp.setDependencies(desc.getDependencies());
        tp.setDeprecated(desc.isDeprecated());
        tp.setDescription(desc.getDescription());
        tp.setDownloadLink(desc.getDownloadLink());
        tp.setEnabled(desc.isEnabled());
        tp.setEndOfAvailability(toDate(desc.getEndOfAvailability()));
        tp.setParent(getTargetPackage(desc.getParent()));
        tp.setRefVersion(desc.getRefVersion());
        tp.setReleaseDate(toDate(desc.getReleaseDate()));
        tp.setRestricted(desc.isRestricted());
        tp.setStatus(desc.getStatus());
        tp.setTypes(desc.getTypes());
        return tp;
    }

    @Override
    public TargetPlatformInstance getTargetPlatformInstance(String id, List<String> packages) {
        if (id == null) {
            return null;
        }

        TargetPlatformInstanceImpl tpi = createTargetPlatformInstanceFromId(id);

        if (packages != null) {
            for (String pkg : packages) {
                TargetPackage tpkg = getTargetPackage(pkg);
                if (tpkg != null) {
                    tpi.addEnabledPackage(tpkg);
                } else {
                    log.warn(String.format("Referenced target package '%s' not found.", pkg));
                }
            }
        }

        return tpi;
    }

    @Override
    public List<TargetPlatform> getAvailableTargetPlatforms(TargetPlatformFilter filter) {
        List<TargetPlatform> tps = new ArrayList<>();
        for (TargetPlatformDescriptor desc : platforms.getTargetPlatforms()) {
            TargetPlatform tp = getTargetPlatform(desc);
            if (tp == null) {
                continue;
            }
            if (filter != null && !filter.accepts(tp)) {
                continue;
            }
            tps.add(tp);
        }
        // always sort for a deterministic result
        Collections.sort(tps, new Comparator<TargetPlatform>() {
            @Override
            public int compare(TargetPlatform arg0, TargetPlatform arg1) {
                return arg0.getId().compareTo(arg1.getId());
            }
        });
        return tps;
    }

    @Override
    public List<TargetPlatformInfo> getAvailableTargetPlatformsInfo(TargetPlatformFilter filter) {
        List<TargetPlatformInfo> tps = new ArrayList<>();
        for (TargetPlatformDescriptor desc : platforms.getTargetPlatforms()) {
            TargetPlatformInfo tp = getTargetPlatformInfo(desc);
            if (tp == null) {
                continue;
            }
            if (filter != null && !filter.accepts(tp)) {
                continue;
            }
            tps.add(tp);
        }
        Collections.sort(tps, new Comparator<TargetPlatformInfo>() {
            @Override
            public int compare(TargetPlatformInfo arg0, TargetPlatformInfo arg1) {
                return arg0.getId().compareTo(arg1.getId());
            }
        });
        return tps;
    }

    @Override
    public void deprecateTargetPlatform(boolean deprecate, final String id) {
        Integer val = deprecate ? Integer.valueOf(1) : Integer.valueOf(0);
        updateOrCreateEntry(id, DirectoryUpdater.DEPRECATED_PROP, val);
    }

    @Override
    public void enableTargetPlatform(boolean enable, final String id) {
        Integer val = enable ? Integer.valueOf(1) : Integer.valueOf(0);
        updateOrCreateEntry(id, DirectoryUpdater.ENABLED_PROP, val);
    }

    @Override
    public void restrictTargetPlatform(boolean restrict, final String id) {
        Integer val = restrict ? Integer.valueOf(1) : Integer.valueOf(0);
        updateOrCreateEntry(id, DirectoryUpdater.RESTRICTED_PROP, val);
    }

    @Override
    public void setTrialTargetPlatform(boolean trial, final String id) {
        Integer val = trial ? Integer.valueOf(1) : Integer.valueOf(0);
        updateOrCreateEntry(id, DirectoryUpdater.TRIAL_PROP, val);
    }

    @Override
    public void setDefaultTargetPlatform(boolean isDefault, final String id) {
        Integer val = isDefault ? Integer.valueOf(1) : Integer.valueOf(0);
        updateOrCreateEntry(id, DirectoryUpdater.DEFAULT_PROP, val);
    }

    @Override
    public void restoreTargetPlatform(final String id) {
        new DirectoryUpdater(getOverrideDirectory()) {
            @Override
            public void run(DirectoryService service, Session session) {
                session.deleteEntry(id);
            }
        }.run();
    }

    @Override
    public void restoreAllTargetPlatforms() {
        new DirectoryUpdater(getOverrideDirectory()) {
            @Override
            public void run(DirectoryService service, Session session) {
                for (DocumentModel entry : session.getEntries()) {
                    session.deleteEntry(entry.getId());
                }
            }
        }.run();
    }

    protected void updateOrCreateEntry(final String id, final String prop, final Integer value) {
        new DirectoryUpdater(getOverrideDirectory()) {
            @Override
            public void run(DirectoryService service, Session session) {
                DocumentModel doc = session.getEntry(id);
                if (doc != null) {
                    doc.setProperty(DirectoryUpdater.SCHEMA, prop, value);
                    session.updateEntry(doc);
                } else {
                    DocumentModel entry = BaseSession.createEntryModel(null, DirectoryUpdater.SCHEMA, null, null);
                    entry.setProperty(DirectoryUpdater.SCHEMA, prop, value);
                    entry.setProperty(DirectoryUpdater.SCHEMA, "id", id);
                    session.createEntry(entry);
                }
            }
        }.run();
    }

    protected DocumentModel getDirectoryEntry(String id) {
        Session dirSession = null;
        try {
            // check if entry already exists
            DirectoryService dirService = Framework.getService(DirectoryService.class);
            String dirName = getOverrideDirectory();
            dirSession = dirService.open(dirName);
            return dirSession.getEntry(id);
        } finally {
            if (dirSession != null) {
                dirSession.close();
            }
        }
    }

    @Override
    public TargetPlatformInstance getDefaultTargetPlatformInstance(boolean restricted) {
        TargetPlatformInstance tpi = null;
        TargetPlatformFilterImpl filter = new TargetPlatformFilterImpl();
        filter.setFilterRestricted(restricted);
        TargetPlatform defaultTP = getDefaultTargetPlatform(filter);
        if (defaultTP != null) {
            tpi = createTargetPlatformInstanceFromId(defaultTP.getId());
        }

        return tpi;
    }

    /**
     * Create a TargetPlatformInstance given an id.
     *
     * @since 5.9.3-NXP-15602
     */
    protected TargetPlatformInstanceImpl createTargetPlatformInstanceFromId(String id) {
        TargetPlatformDescriptor desc = platforms.getTargetPlatform(id);
        if (desc == null) {
            return null;
        }
        TargetPlatformInstanceImpl tpi = new TargetPlatformInstanceImpl(id, desc.getName(), desc.getVersion(),
                desc.getRefVersion(), desc.getLabel());
        tpi.setDeprecated(desc.isDeprecated());
        tpi.setDescription(desc.getDescription());
        tpi.setDownloadLink(desc.getDownloadLink());
        tpi.setEnabled(desc.isEnabled());
        tpi.setEndOfAvailability(toDate(desc.getEndOfAvailability()));
        tpi.setFastTrack(desc.isFastTrack());
        tpi.setParent(getTargetPlatform(desc.getParent()));
        tpi.setRefVersion(desc.getRefVersion());
        tpi.setReleaseDate(toDate(desc.getReleaseDate()));
        tpi.setRestricted(desc.isRestricted());
        tpi.setStatus(desc.getStatus());
        tpi.setTypes(desc.getTypes());

        DocumentModel entry = getDirectoryEntry(id);
        if (entry != null) {
            Long enabled = (Long) entry.getProperty(DirectoryUpdater.SCHEMA, DirectoryUpdater.ENABLED_PROP);
            if (enabled != null && enabled.intValue() >= 0) {
                tpi.setEnabled(enabled.intValue() == 0 ? false : true);
            }
            Long restricted = (Long) entry.getProperty(DirectoryUpdater.SCHEMA, DirectoryUpdater.RESTRICTED_PROP);
            if (restricted != null && restricted.intValue() >= 0) {
                tpi.setRestricted(restricted.intValue() == 0 ? false : true);
            }
            Long deprecated = (Long) entry.getProperty(DirectoryUpdater.SCHEMA, DirectoryUpdater.DEPRECATED_PROP);
            if (deprecated != null && deprecated.intValue() >= 0) {
                tpi.setDeprecated(deprecated.intValue() == 0 ? false : true);
            }
            tpi.setOverridden(true);
        }

        return tpi;
    }
}
