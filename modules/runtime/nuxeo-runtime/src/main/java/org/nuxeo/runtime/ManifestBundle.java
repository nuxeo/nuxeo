/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.runtime;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.substringBefore;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import org.nuxeo.common.utils.JarUtils;

/**
 * Bundle defined by a {@code MANIFEST.MF} file.
 *
 * @since 11.1
 */
public class ManifestBundle implements Bundle {

    protected static final Name BUNDLE_SYMBOLIC_NAME = new Name("Bundle-SymbolicName");

    protected static final Name NUXEO_COMPONENT = new Name("Nuxeo-Component");

    protected final String name;

    protected final List<String> components;

    protected final File file;

    protected ManifestBundle(String name, File file, List<String> components) {
        this.name = name;
        this.components = components;
        this.file = file;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<String> getDeclaredComponents() {
        return components;
    }

    @Override
    public File getFile() {
        return file;
    }

    public static Optional<ManifestBundle> from(File file) {
        Manifest mf = JarUtils.getManifest(file);
        if (mf == null || !mf.getMainAttributes().containsKey(BUNDLE_SYMBOLIC_NAME)) {
            // not a valid manifest bundle
            return Optional.empty();
        }
        String name = substringBefore(mf.getMainAttributes().getValue(BUNDLE_SYMBOLIC_NAME), ";");
        String nuxeoComponents = mf.getMainAttributes().getValue(NUXEO_COMPONENT);
        List<String> components;
        if (isBlank(nuxeoComponents)) {
            components = List.of();
        } else {
            components = List.of(mf.getMainAttributes().getValue(NUXEO_COMPONENT).split(",\\s*"));
        }
        return Optional.of(new ManifestBundle(name, file, components));
    }
}
