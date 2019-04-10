package org.nuxeo.apidoc.snapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnapshotResolverHelper {

    protected static final String[] capaliases = { "cap", "Nuxeo Platform",
            "Nuxeo cap", "Nuxeo DM", "dm" };

    public static String findBestMatch(List<DistributionSnapshot> snaps,
            String distributionId) {

        if (distributionId == null || "".equals(distributionId.trim())) {
            return null;
        }
        if ("current".equalsIgnoreCase((distributionId.trim()))) {
            return "current";
        }

        // exact match
        for (DistributionSnapshot snap : snaps) {
            if (snap.getKey().equalsIgnoreCase(distributionId)) {
                return snap.getKey();
            }
        }

        // name match + best version
        String[] parts = distributionId.split("-");
        if (parts.length > 1) {
            String name = parts[0];
            String version = distributionId.replace(name + "-", "");
            name = getName(name);
            List<String> potentialVersions = new ArrayList<String>();
            Map<String, String> dist4Version = new HashMap<String, String>();
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
            if (idx == potentialVersions.size() - 1) {
                targetVersion = potentialVersions.get(idx - 1);
            } else if (idx < potentialVersions.size() - 1) {
                targetVersion = potentialVersions.get(idx + 1);
            }

            if (targetVersion != null) {
                return dist4Version.get(targetVersion) + "-" + targetVersion;
            }
        }
        return null;
    }

    protected static String getName(String name) {
        for (String dname : capaliases) {
            if (dname.equalsIgnoreCase(name)) {
                return "cap";
            }
        }
        return name;
    }

}
