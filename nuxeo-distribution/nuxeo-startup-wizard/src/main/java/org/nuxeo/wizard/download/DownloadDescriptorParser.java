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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    protected static final Log log = LogFactory.getLog(DownloadDescriptorParser.class);

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

            String baseUrl = document.getRootElement().element(
                    "packageDefinitions").attributeValue("baseUrl");

            // parse package definition
            for (Object el : document.getRootElement().element(
                    "packageDefinitions").elements("package")) {
                DownloadPackage pkg = readPackageDefinition((Element) el,
                        baseUrl);
                if (pkg != null) {
                    pkgs.add(pkg);
                }
            }

            options.setAllPackages(pkgs);

            Element install = document.getRootElement().element("install");

            // get common packages
            if (install.element("common") != null) {
                for (Object el : install.element("common").elements("package")) {
                    DownloadPackage pkg = readCommonPackage((Element) el, pkgs);
                    if (pkg != null) {
                        options.addCommonPackage(pkg);
                    }
                }
            }

            nodeCounter = 0;
            // get package Options
            for (Object el : install.element("packageOptions").elements(
                    "package")) {
                DownloadablePackageOption pkg = readPackageOptions(
                        (Element) el, pkgs);
                if (pkg != null) {
                    options.addOptions(pkg);
                }
            }

            // get presets
            if (document.getRootElement().element("presets") != null) {
                for (Object el : document.getRootElement().element("presets").elements(
                        "preset")) {
                    Element preset = (Element) el;
                    String presetId = preset.attribute("id").getValue();
                    String presetLabel = preset.attribute("label").getValue();
                    String pkgList = preset.getText();
                    String[] presetPackages = pkgList.split(",");
                    options.addPreset(presetId, presetLabel, presetPackages);
                }
            }
        }
        return options;
    }

    protected static int nodeCounter = 0;

    protected static DownloadPackage readPackageDefinition(Element el,
            String baseUrl) {
        String id = el.attribute("id").getValue();
        if (id != null) {
            DownloadPackage pkg = new DownloadPackage(id);
            String bUrl = el.attributeValue("baseUrl");
            if (bUrl == null) {
                bUrl = baseUrl;
            }
            pkg.setLabel(el.attributeValue("label"));
            pkg.setFilename(el.attributeValue("filename"));
            pkg.setMd5(el.attributeValue("md5"));
            pkg.setVirtual(Boolean.parseBoolean(el.attributeValue("virtual")));
            pkg.setBaseUrl(bUrl);
            pkg.setColor(el.attributeValue("color"));
            pkg.setTextColor(el.attributeValue("textcolor"));
            pkg.setShortLabel(el.attributeValue("shortlabel"));
            String url = el.attributeValue("url");
            if (url != null) {
                pkg.setDownloadUrl(url);
            }

            String implies = el.attributeValue("implies");
            if (implies != null && !implies.trim().equals("")) {
                String[] deps = implies.split(",");
                pkg.addDeps(deps);
            }
            return pkg;
        }
        return null;
    }

    protected static DownloadPackage readCommonPackage(Element el,
            List<DownloadPackage> pkgs) {
        String ref = el.attributeValue("ref");
        for (DownloadPackage pkg : pkgs) {
            if (pkg.getId().equals(ref)) {
                return pkg;
            }
        }
        log.error("Unable to find common package for ref " + ref);
        return null;
    }

    protected static DownloadablePackageOption readPackageOptions(Element el,
            List<DownloadPackage> pkgs) {

        String ref = el.attributeValue("ref");
        DownloadPackage targetPkg = null;

        if (ref != null) {
            for (DownloadPackage pkg : pkgs) {
                if (pkg.getId().equals(ref)) {
                    targetPkg = pkg;
                    break;
                }
            }
            if (targetPkg == null) {
                log.error("Unable to find package for ref " + ref);
                return null;
            }
        }

        String id = el.attributeValue("ref");
        if (id == null) {
            id = ref;
        }
        DownloadablePackageOption pkgOption;
        nodeCounter++;

        if (id != null) {
            pkgOption = new DownloadablePackageOption(targetPkg, id);
        } else {
            pkgOption = new DownloadablePackageOption(targetPkg, nodeCounter);
        }

        String label = el.attributeValue("label");
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
}
