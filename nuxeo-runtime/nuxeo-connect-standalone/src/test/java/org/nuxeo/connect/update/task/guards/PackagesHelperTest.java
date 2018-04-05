/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Mincong Huang
 */
package org.nuxeo.connect.update.task.guards;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageUpdateService;

public class PackagesHelperTest {

    @Mock
    private PackageUpdateService service;

    @Mock
    private LocalPackage packageA1;

    @Mock
    private LocalPackage packageA2;

    @InjectMocks
    private PackagesHelper helper;

    private final List<LocalPackage> packages = new ArrayList<>();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        helper = new PackagesHelper(service);
        packages.add(packageA1);
        packages.add(packageA2);
    }

    @After
    public void tearDown() {
        packages.clear();
    }

    // NXP-24671
    @Test
    public void contains_packagesWithSameName() throws Exception {
        // Given packages `A.1` and `A.2` having different ids, but the same name `A`.
        Mockito.when(packageA1.getName()).thenReturn("A");
        Mockito.when(packageA2.getName()).thenReturn("A");
        Mockito.when(packageA1.getPackageState()).thenReturn(PackageState.DOWNLOADED);
        Mockito.when(packageA2.getPackageState()).thenReturn(PackageState.INSTALLED);

        Mockito.when(service.getPackages()).thenReturn(packages);

        // When asking if there's any package `A` installed
        // Then helper should return true because `A.2` is installed
        assertThat(helper.contains("A")).isTrue();
    }
}
