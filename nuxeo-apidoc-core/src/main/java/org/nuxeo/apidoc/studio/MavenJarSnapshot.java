package org.nuxeo.apidoc.studio;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.apidoc.introspection.AbstractRuntimeSnapshot;
import org.nuxeo.apidoc.introspection.BundleInfoImpl;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;

public class MavenJarSnapshot extends AbstractRuntimeSnapshot implements
        DistributionSnapshot {

    protected String version;

    protected String name;

    protected BundleInfoImpl bundle;

    public static MavenJarSnapshot resolve(String id) {
        if (id.startsWith("mvn:")) {
            id = id.substring(4);
        }
        String[] parts = id.split("|");
        return new MavenJarSnapshot(parts[0], parts[1], parts[2]);
    }

    public MavenJarSnapshot(String grpId, String artifactId, String version) {
        this.version = version;
        this.name = "mvn:" + grpId + "|" + artifactId + "|" + version;

        File jar = fetchArtifact(grpId, artifactId, version);
        bundle = new StudioBundleInfo(jar);
        bundle.setArtifactId(artifactId);
        bundle.setGroupId(grpId);
        bundle.setArtifactVersion(version);
        initSnapshot();
    }

    protected File fetchArtifact(String grpId, String artifactId, String version) {
        return new File("/home/tiry/testStudio.jar");
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void initOperations() {
    }

    @Override
    protected Collection<BundleInfoImpl> getBundles() {
        List<BundleInfoImpl> bs = new ArrayList<BundleInfoImpl>();
        bs.add(bundle);
        return bs;
    }

}
