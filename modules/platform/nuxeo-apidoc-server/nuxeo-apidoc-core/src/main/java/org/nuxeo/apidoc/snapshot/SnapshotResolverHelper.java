/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.snapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

public class SnapshotResolverHelper {

    protected static final String[] capaliases = { "cap", "Nuxeo Platform", "Nuxeo cap", "Nuxeo DM", "dm" };

    public static String findBestMatch(List<DistributionSnapshot> snaps, String distributionId) {

        if (StringUtils.isBlank(distributionId)) {
            return null;
        }

        // exact match
        for (DistributionSnapshot snap : snaps) {
            if (snap.getKey().equalsIgnoreCase(distributionId)) {
                return snap.getKey();
            }
        }

        // aliases
        Optional<DistributionSnapshot> first = snaps.stream()
                                                    .filter(s -> s.getAliases().contains(distributionId))
                                                    .findFirst();
        if (first.isPresent()) {
            return first.get().getKey();
        }

        // name match + best version
        String[] parts = distributionId.split("-");
        if (parts.length > 1) {
            String name = parts[0];
            String version = distributionId.replace(name + "-", "");
            name = getName(name);
            List<String> potentialVersions = new ArrayList<>();
            Map<String, String> dist4Version = new HashMap<>();
            for (DistributionSnapshot snap : snaps) {
                if (getName(snap.getName()).equalsIgnoreCase(name)) {
                    potentialVersions.add(snap.getVersion());
                    dist4Version.put(snap.getVersion(), snap.getName());
                    if (snap.getVersion().equals(version)) {
                        return snap.getKey();
                    }
                }
            }

            potentialVersions.add(version);
            Collections.sort(potentialVersions);
            int idx = potentialVersions.indexOf(version);

            String targetVersion = null;
            if (idx > 0) {
                if (idx == potentialVersions.size() - 1) {
                    targetVersion = potentialVersions.get(idx - 1);
                } else if (idx < potentialVersions.size() - 1) {
                    targetVersion = potentialVersions.get(idx + 1);
                }
            }

            if (targetVersion != null) {
                return dist4Version.get(targetVersion) + "-" + targetVersion;
            }
        }
        return null;
    }

    protected static String getName(String name) {
        Optional<String> first = Arrays.stream(capaliases)
                                       .filter(s -> name.toLowerCase().startsWith(s.toLowerCase()))
                                       .findFirst();
        return first.isPresent() ? "cap" : name;
    }

}
