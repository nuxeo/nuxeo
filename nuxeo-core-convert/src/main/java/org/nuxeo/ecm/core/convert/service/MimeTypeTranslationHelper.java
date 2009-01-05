package org.nuxeo.ecm.core.convert.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

public class MimeTypeTranslationHelper {

    protected static Map<String, List<ConvertOption>> srcMappings = new HashMap<String, List<ConvertOption>>();
    protected static Map<String, List<ConvertOption>> dstMappings = new HashMap<String, List<ConvertOption>>();


    public static  void addConverter(ConverterDescriptor desc) {

        List<String> sMts = desc.getSourceMimeTypes();
        String dMt = desc.getDestinationMimeType();

        List<ConvertOption> dco = dstMappings.get(dMt);
        if (dco==null) {
            dco = new ArrayList<ConvertOption>();
        }

        for (String sMT : sMts) {

            List<ConvertOption> sco = srcMappings.get(sMT);

            if (sco==null) {
                sco = new ArrayList<ConvertOption>();
            }

            sco.add(new ConvertOption(desc.getConverterName(),dMt));
            srcMappings.put(sMT, sco);

            dco.add(new ConvertOption(desc.getConverterName(),sMT));
        }
        dstMappings.put(dMt, dco);
    }


    public static String getConverterName(String sourceMimeType, String destMimeType) {

        List<ConvertOption> sco = srcMappings.get(sourceMimeType);
        if (sco==null) {
            return null;
        }
        for (ConvertOption co : sco) {
            if (co.mimeType.equals(destMimeType)) {
                return co.getConverterName();
            }
        }

        // try with wildcards
        sco = srcMappings.get("*");
        for (ConvertOption co : sco) {
            if (co.mimeType.equals(destMimeType)) {
                return co.getConverterName();
            }
        }

        return null;
    }

    public static List<String> getDestinationMimeTypes(String sourceMimeType) {
        List<String> dst = new ArrayList<String>();

        List<ConvertOption> sco = srcMappings.get(sourceMimeType);

        for (ConvertOption co : sco) {
            dst.add(co.getMimeType());
        }
        return dst;
    }

    public static List<String> getSourceMimeTypes(String destinationMimeType) {
        List<String> src = new ArrayList<String>();

        List<ConvertOption> dco = srcMappings.get(destinationMimeType);

        for (ConvertOption co : dco) {
            src.add(co.getMimeType());
        }
        return src;

    }

}
