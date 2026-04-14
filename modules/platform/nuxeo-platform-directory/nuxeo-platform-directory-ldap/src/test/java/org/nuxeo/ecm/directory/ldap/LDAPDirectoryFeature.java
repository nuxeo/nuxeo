/*
 * (C) Copyright 2006-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.directory.ldap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.nuxeo.ecm.directory.sql.SQLDirectoryFeature;
import org.nuxeo.runtime.test.runner.BlacklistComponent;
import org.nuxeo.runtime.test.runner.Defaults;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogFeature;
import org.nuxeo.runtime.test.runner.LoggerLevel;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * This feature deploy the required LDAP directory contributions and enable the user to configure LDAP tests through the
 * #LDAPDirectoryFeature.Config annotation
 *
 * @author bogdan
 * @since 9.2
 */
@Features({ LogFeature.class, SQLDirectoryFeature.class })
@Deploy("org.nuxeo.ecm.directory")
@Deploy("org.nuxeo.ecm.directory.ldap")
@Deploy("org.nuxeo.ecm.directory.ldap.tests")
@Deploy("org.nuxeo.ecm.directory.ldap.tests:ldap-test-setup/DirectoryTypes.xml")
@Deploy("org.nuxeo.ecm.directory.ldap.tests:TestSQLDirectories.xml")
@BlacklistComponent("org.nuxeo.ecm.directory.ldap.management") // needs CoreManagementComponent
// ignore warning explaining that admin password needs to be changed, as it cannot be changed before startup
@LoggerLevel(name = "org.apache.directory.server.core.DefaultDirectoryService", level = "ERROR")
public class LDAPDirectoryFeature implements RunnerFeature {

    /**
     * Can be used to change the local server setup file. The default setup file is
     * <code>TestDirectoriesWithInternalApacheDS.xml</code>
     *
     * @author bogdan
     */
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface LocalServerSetup {

        /**
         * The configuration file to deploy
         */
        String value() default "TestDirectoriesWithInternalApacheDS.xml";
    }

    /**
     * Use this annotation on the test class when willing to test against an external server. If this annotation is used
     * the LocalServerSetup will have no effect
     *
     * @author bogdan
     */
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface UseExternalServer {

        /**
         * Change this flag in case the external LDAP server considers the posixGroup class structural
         */
        boolean isPosixGroupStructural() default true;

        /**
         * change this flag if your test server has support for dynamic groups through the groupOfURLs objectclass.
         */
        boolean hasDynGroupSchema() default false;

        /**
         * the configuration file to deploy
         */
        String value() default "TestDirectoriesWithExternalOpenLDAP.xml";
    }

    protected boolean isExternal = false;

    protected UseExternalServer externalServerConfig;

    protected LocalServerSetup localServerSetup;

    protected RuntimeHarness harness;

    protected MockLdapServer server;

    @Override
    public void initialize(FeaturesRunner runner) {
        harness = runner.getFeature(RuntimeFeature.class).getHarness();
        externalServerConfig = runner.getTargetTestClass().getAnnotation(UseExternalServer.class);
        if (externalServerConfig == null) {
            externalServerConfig = Defaults.of(UseExternalServer.class);
            isExternal = false;
        } else {
            isExternal = true;
        }
        localServerSetup = runner.getTargetTestClass().getAnnotation(LocalServerSetup.class);
        if (localServerSetup == null) {
            localServerSetup = Defaults.of(LocalServerSetup.class);
        }
    }

    public String getSetupFile() {
        if (isExternal) {
            return externalServerConfig.value();
        } else {
            return localServerSetup.value();
        }
    }

    public boolean isExternal() {
        return isExternal;
    }

    public boolean isPosixGroupStructural() {
        return externalServerConfig.isPosixGroupStructural();
    }

    public boolean hasDynGroupSchema() {
        return externalServerConfig.hasDynGroupSchema();
    }

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        harness.deployContrib("org.nuxeo.ecm.directory.ldap.tests", getSetupFile());
    }

}
