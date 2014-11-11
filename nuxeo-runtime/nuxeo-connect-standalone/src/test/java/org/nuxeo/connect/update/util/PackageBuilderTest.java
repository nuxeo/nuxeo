package org.nuxeo.connect.update.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.xml.PackageDefinitionImpl;
import org.nuxeo.connect.update.standalone.StandaloneUpdateService;


public class PackageBuilderTest {

    @Test
    public void testPackageBuilder() {
        PackageBuilder builder = new PackageBuilder();
        builder.name("nuxeo-automation").version("5.3.2").type(
                PackageType.ADDON);
        builder.title("Nuxeo Automation").description(
                "The automation framework");
        builder.platform("dm-5.3.2");
        builder.dependency("nuxeo-core:5.3.2");
        builder.conflict("package-that-does-not-exist");
        builder.provide("virtual-package");
        builder.classifier("OpenSource");
        builder.installer("MyInstaller", true);
        builder.addLicense("My License");

        String xml = builder.buildManifest();
        // System.out.println(xml);

        XMap xmap = StandaloneUpdateService.createXmap();
        try {
            PackageDefinitionImpl pdef = (PackageDefinitionImpl) xmap.load(new ByteArrayInputStream(
                xml.getBytes()));
            // System.out.println(pdef);
        } catch (Exception e) {
            fail("Could not create package definition");
        }

        try {
            File file = builder.build();
            assertTrue(file.exists());
            file.delete();
        } catch (IOException e) {
            fail("Coud not create package file");
        }
    }

}
