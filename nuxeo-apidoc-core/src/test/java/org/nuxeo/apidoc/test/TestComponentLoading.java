package org.nuxeo.apidoc.test;

import java.io.File;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.api.BundleGroupFlatTree;
import org.nuxeo.apidoc.api.BundleGroupTreeHelper;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.documentation.ContributionItem;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.studio.MavenJarSnapshot;
import org.nuxeo.apidoc.studio.StudioBundleInfo;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.impl.RegistrationInfoImpl;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestComponentLoading {

    protected String dumpSnapshot(DistributionSnapshot snap) {

        StringBuilder sb = new StringBuilder();

        BundleGroupTreeHelper bgth = new BundleGroupTreeHelper(snap);

        List<BundleGroupFlatTree> tree = bgth.getBundleGroupTree();
        for (BundleGroupFlatTree info : tree) {
            String pad = " ";
            for (int i = 0; i <= info.getLevel(); i++) {
                pad += " ";
            }
            sb.append(pad + "- " + info.getGroup().getName() + "("
                    + info.getGroup().getId() + ")");
            sb.append(" *** ");
            sb.append(info.getGroup().getHierarchyPath());
            sb.append("\n");
        }

        List<String> bids = snap.getBundleIds();
        List<String> cids = snap.getComponentIds();
        List<String> sids = snap.getServiceIds();
        List<String> epids = snap.getExtensionPointIds();
        List<String> exids = snap.getContributionIds();

        Collections.sort(bids);
        Collections.sort(cids);
        Collections.sort(sids);
        Collections.sort(epids);
        Collections.sort(exids);

        for (String bid : bids) {
            sb.append("bundle : " + bid);
            BundleInfo bi = snap.getBundle(bid);
            sb.append(" *** ");
            sb.append(bi.getHierarchyPath());
            sb.append("\n");
        }

        for (String cid : cids) {
            sb.append("component : " + cid);
            sb.append(" *** ");
            ComponentInfo ci = snap.getComponent(cid);
            sb.append(ci.getHierarchyPath());
            sb.append("\n");
        }

        for (String sid : sids) {
            sb.append("service : " + sid);
            sb.append(" *** ");
            ServiceInfo si = snap.getService(sid);
            sb.append(si.getHierarchyPath());
            sb.append("\n");
        }

        for (String epid : epids) {
            sb.append("extensionPoint : " + epid);
            sb.append(" *** ");
            ExtensionPointInfo epi = snap.getExtensionPoint(epid);
            sb.append(epi.getHierarchyPath());
            sb.append("\n");

        }

        for (String exid : exids) {
            sb.append("contribution : " + exid);
            sb.append(" *** ");
            ExtensionInfo exi = snap.getContribution(exid);
            sb.append(exi.getHierarchyPath());
            sb.append("\n");
        }

        return sb.toString();
    }

    @Test
    public void testOpenStudioJar() throws Exception {

        File jar = new File("/home/tiry/testStudio.jar");
        Assert.assertTrue(jar.exists());

        StudioBundleInfo bundle = new StudioBundleInfo(jar);

        System.out.println(bundle);

        ComponentInfo ci = bundle.getComponents().iterator().next();

        System.out.println(ci.getId());

        for (ExtensionInfo ei : ci.getExtensions()) {

            System.out.println(ei.getExtensionPoint());
            // System.out.println(ei.getXml());
            for (ContributionItem fragment : ei.getContributionItems()) {
                System.out.println("    #####");
                System.out.println(fragment.getRawXml());
            }
        }

        MavenJarSnapshot jarSnapshot = new MavenJarSnapshot("grp", "artiId",
                "1.0");
        System.out.println(dumpSnapshot(jarSnapshot));

    }
}
