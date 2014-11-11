/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     tdelprat
 *
 */
package org.nuxeo.wizard.download;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * Simple DOM4J parser to read the {@link DownloadPackage} list from an XML
 * stream
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class DownloadDescriptorParser {

    public static Document parse(InputStream in) {
        Document document = null;
        SAXReader reader = new SAXReader();
        try {
            document = reader.read(in);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return document;
    }

    public static DownloadablePackageOptions parsePackages(InputStream in) {

        DownloadablePackageOptions options = new DownloadablePackageOptions();

        List<DownloadPackage> pkgs = new ArrayList<DownloadPackage>();
        Document document = parse(in);
        if (document != null) {

            String baseUrl = document.getRootElement().element("packageDefinitions").attributeValue("baseUrl");

            for (Object el : document.getRootElement().element(
                    "packageDefinitions").elements("package")) {
                DownloadPackage pkg = readPackageDefinition((Element) el, baseUrl);
                if (pkg != null) {
                    pkgs.add(pkg);
                }
            }

            for (Object el : document.getRootElement().element("packageOptions").elements(
                    "package")) {
                DownloadablePackageOption pkg = readPackageOptions(
                        (Element) el, pkgs);
                if (pkg != null) {
                    options.addOptions(pkg);
                }
            }
        }

        return options;
    }

    protected static DownloadPackage readPackageDefinition(Element el, String baseUrl) {
        String id = el.attribute("id").getValue();
        if (id != null) {
            DownloadPackage pkg = new DownloadPackage(id);
            String bUrl = el.attributeValue("baseUrl");
            if (bUrl==null) {
                bUrl = baseUrl;
            }
            pkg.setLabel(el.attributeValue("label"));
            pkg.setFilename(el.attributeValue("filename"));
            pkg.setMd5(el.attributeValue("md5"));
            pkg.setBaseUrl(bUrl);
            return pkg;
        }
        return null;
    }

    protected static DownloadablePackageOption readPackageOptions(Element el,
            List<DownloadPackage> pkgs) {

        String ref = el.attributeValue("ref");
        DownloadPackage targetPkg = null;

        for (DownloadPackage pkg : pkgs) {
            if (pkg.getId().equals(ref)) {
                targetPkg = pkg;
                break;
            }
        }

        if (targetPkg != null) {
            DownloadablePackageOption pkgOption = new DownloadablePackageOption(
                    targetPkg);
            String label = el.attributeValue("lasbel");
            if (label != null) {
                pkgOption.setLabel(label);
            }
            pkgOption.setExclusive(el.attributeValue("exclusive"));

            for (Object child : el.elements()) {
                DownloadablePackageOption childPkg = readPackageOptions(
                        (Element) child, pkgs);
                if (childPkg != null) {
                    pkgOption.addChildPackage(childPkg);
                }
            }
            return pkgOption;
        }
        return null;
    }
}
