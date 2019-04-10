package org.nuxeo.apidoc.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.SeamComponentInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.documentation.JavaDocHelper;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotResolverHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestDistributionResolver {

    protected static final String[] alldistribs = { "cap-5.5", "cap-5.5-RC1", "cap-5.5-SNAPSHOT", "cap-5.6-SNAPSHOT",
            "cap-5.6-RC1", "dm-5.5", "Nuxeo DM-5.4.1", "Nuxeo Platform-5.6-RC1" };

    protected List<DistributionSnapshot> buildFakeSnaps() {

        List<DistributionSnapshot> snaps = new ArrayList<DistributionSnapshot>();
        for (final String distId : alldistribs) {

            DistributionSnapshot snap = new DistributionSnapshot() {

                String id = distId;

                @Override
                public boolean isLive() {
                    // TODO Auto-generated method stub
                    return false;
                }

                @Override
                public String getVersion() {
                    return id.replace(getName() + "-", "");
                }

                @Override
                public String getName() {
                    return distId.split("-")[0];
                }

                @Override
                public Date getCreationDate() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public List<String> getXmlComponentIds() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public List<Class<?>> getSpi() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public List<String> getServiceIds() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public ServiceInfo getService(String id) {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public List<SeamComponentInfo> getSeamComponents() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public List<String> getSeamComponentIds() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public SeamComponentInfo getSeamComponent(String id) {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public List<OperationInfo> getOperations() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public OperationInfo getOperation(String id) {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public String getKey() {
                    return id;
                }

                @Override
                public void cleanPreviousArtifacts() {
                    // empty body
                }

                @Override
                public JavaDocHelper getJavaDocHelper() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public List<String> getJavaComponentIds() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public List<String> getExtensionPointIds() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public ExtensionPointInfo getExtensionPoint(String id) {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public List<ExtensionInfo> getContributions() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public List<String> getContributionIds() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public ExtensionInfo getContribution(String id) {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public List<String> getComponentIds() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public ComponentInfo getComponent(String id) {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public List<String> getBundleIds() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public List<BundleGroup> getBundleGroups() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public List<String> getBundleGroupChildren(String groupId) {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public BundleGroup getBundleGroup(String groupId) {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public BundleInfo getBundle(String id) {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public boolean containsSeamComponents() {
                    // TODO Auto-generated method stub
                    return false;
                }
            };
            snaps.add(snap);
        }

        return snaps;
    }

    protected String testResolution(List<DistributionSnapshot> snaps, String target) {
        String match = SnapshotResolverHelper.findBestMatch(snaps, target);
        assertNotNull(match);
        return match;
    }

    @Test
    public void testResolver() {
        List<DistributionSnapshot> snaps = buildFakeSnaps();
        String res = testResolution(snaps, "dm-5.6");
        assertEquals("Nuxeo Platform-5.6-RC1", res);
        res = testResolution(snaps, "dm-5.5");
        assertEquals("dm-5.5", res);
        res = testResolution(snaps, "cap-5.6");
        assertEquals("Nuxeo Platform-5.6-RC1", res);
        res = testResolution(snaps, "cap-5.6-SNAPSHOT");
        assertEquals("cap-5.6-SNAPSHOT", res);
        res = testResolution(snaps, "cap-5.6-RC1");
        assertEquals("cap-5.6-RC1", res);

    }
}
