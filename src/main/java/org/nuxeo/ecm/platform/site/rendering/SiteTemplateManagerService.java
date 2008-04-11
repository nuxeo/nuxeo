package org.nuxeo.ecm.platform.site.rendering;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.ecm.platform.site.api.SiteAwareObject;
import org.nuxeo.ecm.platform.site.api.SiteTemplateManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.RuntimeContext;

public class SiteTemplateManagerService extends DefaultComponent implements
        SiteTemplateManager, ResourceLocator {

    public static final ComponentName NAME = new ComponentName(
    "org.nuxeo.ecm.platform.site.rendering.SiteTemplateManagerService");

    protected RenderingEngine engine;


    public RenderingEngine getRenderingEngine() {
        return engine;
    }

    private class BindingEntry {
        private String path;
        private String type;


        BindingEntry(String path, String type)
        {
            this.path=path;
            this.type=type;
        }

        @Override
        public String toString()
        {
            return type + ":" + path;
        }

        @Override
        public int hashCode()
        {
            return toString().hashCode();
        }

        @Override
        public boolean equals(Object other)
        {
            if (other instanceof BindingEntry) {
                BindingEntry otherEntry = (BindingEntry) other;
                return toString().equals(otherEntry.toString());
            }
            else
                return false;
        }

        public boolean match (DocumentModel doc)
        {
            if (!doc.getType().equals(type))
            return false;

            if ("*".equals(path) || doc.getPathAsString().contains(path)) // XXX yurk!
                return true;
            return false;
        }
    }

    public static final String TEMPLATE_EP = "template";
    public static final String TEMPLATE_BINDING_EP = "binding";
    public static final String TEMPLATE_TRANSFORMER_EP = "transformer";

    public static final String DYNAMIC_TEMPLATE_PREFIX = "dyn:";

    private static ComponentContext context;

    private static File templateDir;

    private static final Log log = LogFactory.getLog(SiteTemplateManagerService.class);


    private static Map<String,String> templateRegistry = new HashMap<String, String>();

    private static Map<BindingEntry,String> templateBindingRegistry = new HashMap<BindingEntry,String>();

    private static Map<String,String> dynamicTemplateRegistry = new LRUTemplateCachingMap<String, String>(100);

    // Service interface
    public InputStream getTemplateForDoc(DocumentModel doc) {

        String templateName = getTemplateNameForDoc(doc);
        if (templateName==null)
            return null;

        return getTemplateFromName(templateName);
    }

    public InputStream getTemplateFromName(String templateName) {

        File templateFile = new File(templateRegistry.get(templateName));
        try {
            return new FileInputStream(templateFile);
        } catch (FileNotFoundException e) {
           log.error("Unable to open template file", e);
           return null;
        }
    }

    public URL getTemplateUrlForDoc(DocumentModel doc)
    {
        String templateName = getTemplateNameForDoc(doc);
        if (templateName==null)
            return null;
        return getTemplateUrlFromName(templateName);
    }


    public URL getTemplateUrlFromName(String templateName)
    {
        try {
            if (templateName.startsWith(DYNAMIC_TEMPLATE_PREFIX))
            {
                return new URL("file://" + dynamicTemplateRegistry.get(templateName));
            }
            else
                return new URL("file://" + templateRegistry.get(templateName));
        } catch (MalformedURLException e) {
            log.error("error whil generatig resource URL",e);
            return null;
        }
    }

    public String getTemplateNameForDoc(DocumentModel doc) {
        for (BindingEntry entry : templateBindingRegistry.keySet())
        {
            if (entry.match(doc))
            {
                return templateBindingRegistry.get(entry);
            }
        }
        return null;
    }

    public List<String> getTemplateNames()
    {
     List<String> result = new ArrayList<String>();
     result.addAll(templateRegistry.keySet());
     return result;
    }
    // Component interface for tests



    // RT Component and EP Management

    @Override
    public void activate(ComponentContext context) {
        this.context = context;

        String val = (String)context.getPropertyValue("engine", null);
        if (val != null) {
            try {
            engine = (RenderingEngine)context.getRuntimeContext().loadClass(val).newInstance();
            } catch (Exception e) {
                log.error("Failed to load rendering engine from component configuration -> using the default freemarker engine", e);
            }
        }
        if (engine == null) {
            engine = new FreemarkerEngine(); // the default engine
        }
        engine.setResourceLocator(this);
        //engine.setEnvironmentProvider(env) TODO
        if (engine instanceof FreemarkerEngine) {
            FreemarkerEngine fmEngine = (FreemarkerEngine) engine;
            fmEngine.setDocumentView(new SiteDocumentView());
        }

        templateDir = new File(Framework.getRuntime().getHome(),
                "siteTemplates");
        if (!templateDir.isDirectory()) {
            templateDir.mkdirs();
        }
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (TEMPLATE_EP.equals(extensionPoint)) {
            TemplateDescriptor descriptor = (TemplateDescriptor) contribution;
            descriptor.setRtContext(contributor.getContext());
            try {
                registerTemplate(descriptor);
            } catch (Exception e) {
                log.error("Error during template registration : " + descriptor.getName(),e);
            }
        } else if (TEMPLATE_BINDING_EP.equals(extensionPoint)) {
            TemplateBindingDescriptor descriptor = (TemplateBindingDescriptor) contribution;
            try {
                registerTemplateBinding(descriptor);
            } catch (Exception e) {
                log.error("Error during template registration : " + descriptor.toString(),e);
            }
        } else if (TEMPLATE_TRANSFORMER_EP.equals(extensionPoint)) {
            TransformerDescriptor td = (TransformerDescriptor)contribution;
            engine.setTransformer(td.getName(), td.newInstance());
        }
    }


    protected void registerTemplateBinding(TemplateBindingDescriptor descriptor)
    {
        BindingEntry bEntry = new BindingEntry(descriptor.getPath(), descriptor.getDocType());

        templateBindingRegistry.put(bEntry,descriptor.getTemplateName());

        log.info("registred template binding :" + descriptor.toString());
    }

    protected void registerTemplate(TemplateDescriptor descriptor) throws IOException {
        RuntimeContext rtContext = descriptor.getRtContext() == null ? context.getRuntimeContext()
                : descriptor.getRtContext();

        URL url = rtContext.getLocalResource(descriptor.src);
        if (url == null) {
            url = rtContext.getResource(descriptor.src);
        }
        if (url != null) {
            InputStream in = url.openStream();
            try {
                File file = new File(templateDir, descriptor.name
                        + ".tmpl");
                FileUtils.copyToFile(in, file);

                templateRegistry.put(descriptor.name, file.getAbsolutePath());

                log.info("Registered template: " + descriptor.name + " from "
                            + url.toString());

            } finally {
                in.close();
            }

        }

    }

    public String registerDynamicTemplate(SiteAwareObject site, String templateContent)
    {
        // XXX : should check on modificationDate to avoid recreating template
        String templateName=site.getId();
        String templateKey = DYNAMIC_TEMPLATE_PREFIX + templateName;
        File file = new File(templateDir, templateName + ".tmpl");
        try {
            FileUtils.writeFile(file, templateContent);
        } catch (IOException e) {
            log.error ("Unable to register dynamic template ",e);
            return null;
        }

        dynamicTemplateRegistry.put(templateKey, file.getAbsolutePath());

        return templateKey;
    }


    public URL getResource(String templateName) {
        URL templateURL = getTemplateUrlFromName(templateName);
        return templateURL;
    }

}
