package org.nuxeo.apidoc.studio;

import java.io.File;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.nuxeo.apidoc.introspection.BundleInfoImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.impl.ComponentDescriptorReader;
import org.nuxeo.runtime.model.impl.RegistrationInfoImpl;

public class StudioBundleInfo extends BundleInfoImpl {

    protected static final String COMPONENT_FILE = "OSGI-INF/extensions.xml";

    protected RegistrationInfoImpl ri;

    public StudioBundleInfo(File jar) {
        super(readBundleId(jar), jar);
        try {
            readRI(jar);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (ri != null) {
            addComponent(ri);
        }
    }

    protected static String readBundleId(File jar) {
        return "fakeStudioBunel";
    }

    protected void readRI(File jar) throws Exception {
        ZipFile zFile = new ZipFile(jar);
        ZipEntry extensionsEntry = zFile.getEntry(COMPONENT_FILE);
        if (extensionsEntry != null) {
            InputStream stream = zFile.getInputStream(extensionsEntry);
            ComponentDescriptorReader reader = new ComponentDescriptorReader();
            ri = reader.read(Framework.getRuntime().getContext(), stream);
        }
    }
}
