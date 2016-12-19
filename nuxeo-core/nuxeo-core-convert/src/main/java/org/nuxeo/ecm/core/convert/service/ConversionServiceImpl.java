/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.convert.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConversionStatus;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.core.convert.api.ConverterNotAvailable;
import org.nuxeo.ecm.core.convert.api.ConverterNotRegistered;
import org.nuxeo.ecm.core.convert.cache.CacheKeyGenerator;
import org.nuxeo.ecm.core.convert.cache.ConversionCacheHolder;
import org.nuxeo.ecm.core.convert.cache.GCTask;
import org.nuxeo.ecm.core.convert.extension.ChainedConverter;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.convert.extension.ExternalConverter;
import org.nuxeo.ecm.core.convert.extension.GlobalConfigDescriptor;
import org.nuxeo.ecm.core.transientstore.work.TransientStoreWork;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.reload.ReloadService;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;
import org.nuxeo.runtime.services.event.EventService;

/**
 * Runtime Component that also provides the POJO implementation of the {@link ConversionService}.
 *
 * @author tiry
 */
public class ConversionServiceImpl extends DefaultComponent implements ConversionService {

    protected static final Log log = LogFactory.getLog(ConversionServiceImpl.class);

    public static final String CONVERTER_EP = "converter";

    public static final String CONFIG_EP = "configuration";

    protected final Map<String, ConverterDescriptor> converterDescriptors = new HashMap<>();

    protected final MimeTypeTranslationHelper translationHelper = new MimeTypeTranslationHelper();

    protected final GlobalConfigDescriptor config = new GlobalConfigDescriptor();

    protected static ConversionServiceImpl self;

    protected Thread gcThread;

    protected GCTask gcTask;

    ReloadListener reloadListener;

    class ReloadListener implements EventListener {

        @Override
        public void handleEvent(Event event) {
            if (ReloadService.AFTER_RELOAD_EVENT_ID.equals(event.getId())) {
                startGC();
            } else if (ReloadService.BEFORE_RELOAD_EVENT_ID.equals(event.getId())) {
                endGC();
            }
        }

    }

    @Override
    public void activate(ComponentContext context) {
        converterDescriptors.clear();
        translationHelper.clear();
        self = this;
        config.clearCachingDirectory();
        Framework.addListener(new RuntimeServiceListener() {

            @Override
            public void handleEvent(RuntimeServiceEvent event) {
                if (RuntimeServiceEvent.RUNTIME_ABOUT_TO_STOP != event.id) {
                    return;
                }
                Framework.removeListener(this);
                Framework.getService(EventService.class).removeListener(ReloadService.RELOAD_TOPIC, reloadListener);
                endGC();
            }
        });
        Framework.getService(EventService.class).addListener(ReloadService.RELOAD_TOPIC,
                reloadListener = new ReloadListener());
    }

    @Override
    public void deactivate(ComponentContext context) {
        if (config.isCacheEnabled()) {
            ConversionCacheHolder.deleteCache();
        }
        self = null;
        converterDescriptors.clear();
        translationHelper.clear();
    }

    /**
     * Component implementation.
     */
    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {

        if (CONVERTER_EP.equals(extensionPoint)) {
            ConverterDescriptor desc = (ConverterDescriptor) contribution;
            registerConverter(desc);
        } else if (CONFIG_EP.equals(extensionPoint)) {
            GlobalConfigDescriptor desc = (GlobalConfigDescriptor) contribution;
            config.update(desc);
            config.clearCachingDirectory();
        } else {
            log.error("Unable to handle unknown extensionPoint " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
    }

    /* Component API */

    public static Converter getConverter(String converterName) {
        ConverterDescriptor desc = self.converterDescriptors.get(converterName);
        if (desc == null) {
            return null;
        }
        return desc.getConverterInstance();
    }

    public static ConverterDescriptor getConverterDescriptor(String converterName) {
        return self.converterDescriptors.get(converterName);
    }

    public static long getGCIntervalInMinutes() {
        return self.config.getGCInterval();
    }

    public static void setGCIntervalInMinutes(long interval) {
        self.config.setGCInterval(interval);
    }

    public static void registerConverter(ConverterDescriptor desc) {

        if (self.converterDescriptors.containsKey(desc.getConverterName())) {

            ConverterDescriptor existing = self.converterDescriptors.get(desc.getConverterName());
            desc = existing.merge(desc);
        }
        desc.initConverter();
        self.translationHelper.addConverter(desc);
        self.converterDescriptors.put(desc.getConverterName(), desc);
    }

    public static int getMaxCacheSizeInKB() {
        return self.config.getDiskCacheSize();
    }

    public static void setMaxCacheSizeInKB(int size) {
        self.config.setDiskCacheSize(size);
    }

    public static boolean isCacheEnabled() {
        return self.config.isCacheEnabled();
    }

    public static String getCacheBasePath() {
        return self.config.getCachingDirectory();
    }

    /* Service API */

    @Override
    public List<String> getRegistredConverters() {
        List<String> converterNames = new ArrayList<>();
        converterNames.addAll(converterDescriptors.keySet());
        return converterNames;
    }

    @Override
    public BlobHolder convert(String converterName, BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException {

        // set parameters if null to avoid NPE in converters
        if (parameters == null) {
            parameters = new HashMap<>();
        }

        // exist if not registered
        ConverterCheckResult check = isConverterAvailable(converterName);
        if (!check.isAvailable()) {
            // exist is not installed / configured
            throw new ConverterNotAvailable(converterName);
        }

        ConverterDescriptor desc = converterDescriptors.get(converterName);
        if (desc == null) {
            throw new ConversionException("Converter " + converterName + " can not be found");
        }

        String cacheKey = CacheKeyGenerator.computeKey(converterName, blobHolder, parameters);

        BlobHolder result = ConversionCacheHolder.getFromCache(cacheKey);

        if (result == null) {
            Converter converter = desc.getConverterInstance();
            result = converter.convert(blobHolder, parameters);

            if (config.isCacheEnabled()) {
                ConversionCacheHolder.addToCache(cacheKey, result);
            }
        }

        if (result != null) {
            updateResultBlobMimeType(result, desc);
            updateResultBlobFileName(blobHolder, result);
        }

        return result;
    }

    protected void updateResultBlobMimeType(BlobHolder resultBh, ConverterDescriptor desc) {
        Blob mainBlob = resultBh.getBlob();
        if (mainBlob == null) {
            return;
        }
        String mimeType = mainBlob.getMimeType();
        if (StringUtils.isBlank(mimeType) || mimeType.equals("application/octet-stream")) {
            mainBlob.setMimeType(desc.getDestinationMimeType());
        }
    }

    protected void updateResultBlobFileName(BlobHolder srcBh, BlobHolder resultBh) {
        Blob mainBlob = resultBh.getBlob();
        if (mainBlob == null) {
            return;
        }
        String filename = mainBlob.getFilename();
        if (StringUtils.isBlank(filename) || filename.startsWith("nxblob-")) {
            Blob srcBlob = srcBh.getBlob();
            if (srcBlob != null && StringUtils.isNotBlank(srcBlob.getFilename())) {
                String baseName = FilenameUtils.getBaseName(srcBlob.getFilename());

                MimetypeRegistry mimetypeRegistry = Framework.getLocalService(MimetypeRegistry.class);
                MimetypeEntry mimeTypeEntry = mimetypeRegistry.getMimetypeEntryByMimeType(mainBlob.getMimeType());
                List<String> extensions = mimeTypeEntry.getExtensions();
                String extension;
                if (!extensions.isEmpty()) {
                    extension = extensions.get(0);
                } else {
                    extension = FilenameUtils.getExtension(filename);
                    if (extension == null) {
                        extension = "bin";
                    }
                }
                mainBlob.setFilename(baseName + "." + extension);
            }

        }
    }

    @Override
    public BlobHolder convertToMimeType(String destinationMimeType, BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {
        String srcMt = blobHolder.getBlob().getMimeType();
        String converterName = translationHelper.getConverterName(srcMt, destinationMimeType);
        if (converterName == null) {
            throw new ConversionException(
                    "Cannot find converter from type " + srcMt + " to type " + destinationMimeType);
        }
        return convert(converterName, blobHolder, parameters);
    }

    @Override
    public List<String> getConverterNames(String sourceMimeType, String destinationMimeType) {
        return translationHelper.getConverterNames(sourceMimeType, destinationMimeType);
    }

    @Override
    public String getConverterName(String sourceMimeType, String destinationMimeType) {
        List<String> converterNames = getConverterNames(sourceMimeType, destinationMimeType);
        if (!converterNames.isEmpty()) {
            return converterNames.get(converterNames.size() - 1);
        }
        return null;
    }

    @Override
    public ConverterCheckResult isConverterAvailable(String converterName) throws ConversionException {
        return isConverterAvailable(converterName, false);
    }

    protected final Map<String, ConverterCheckResult> checkResultCache = new HashMap<>();

    @Override
    public ConverterCheckResult isConverterAvailable(String converterName, boolean refresh)
            throws ConverterNotRegistered {

        if (!refresh) {
            if (checkResultCache.containsKey(converterName)) {
                return checkResultCache.get(converterName);
            }
        }

        ConverterDescriptor descriptor = converterDescriptors.get(converterName);
        if (descriptor == null) {
            throw new ConverterNotRegistered(converterName);
        }

        Converter converter = descriptor.getConverterInstance();

        ConverterCheckResult result;
        if (converter instanceof ExternalConverter) {
            ExternalConverter exConverter = (ExternalConverter) converter;
            result = exConverter.isConverterAvailable();
        } else if (converter instanceof ChainedConverter) {
            ChainedConverter chainedConverter = (ChainedConverter) converter;
            result = new ConverterCheckResult();
            if (chainedConverter.isSubConvertersBased()) {
                for (String subConverterName : chainedConverter.getSubConverters()) {
                    result = isConverterAvailable(subConverterName, refresh);
                    if (!result.isAvailable()) {
                        break;
                    }
                }
            }
        } else {
            // return success since there is nothing to test
            result = new ConverterCheckResult();
        }

        result.setSupportedInputMimeTypes(descriptor.getSourceMimeTypes());
        checkResultCache.put(converterName, result);

        return result;
    }

    @Override
    public boolean isSourceMimeTypeSupported(String converterName, String sourceMimeType) {
        return getConverterDescriptor(converterName).getSourceMimeTypes().contains(sourceMimeType);
    }

    @Override
    public String scheduleConversion(String converterName, BlobHolder blobHolder,
            Map<String, Serializable> parameters) {
        WorkManager workManager = Framework.getService(WorkManager.class);
        ConversionWork work = new ConversionWork(converterName, null, blobHolder, parameters);
        workManager.schedule(work);
        return work.getId();
    }

    @Override
    public String scheduleConversionToMimeType(String destinationMimeType, BlobHolder blobHolder,
            Map<String, Serializable> parameters) {
        WorkManager workManager = Framework.getService(WorkManager.class);
        ConversionWork work = new ConversionWork(null, destinationMimeType, blobHolder, parameters);
        workManager.schedule(work);
        return work.getId();
    }

    @Override
    public ConversionStatus getConversionStatus(String id) {
        WorkManager workManager = Framework.getService(WorkManager.class);
        Work.State workState = workManager.getWorkState(id);
        if (workState == null) {
            String entryKey = TransientStoreWork.computeEntryKey(id);
            if (TransientStoreWork.containsBlobHolder(entryKey)) {
                return new ConversionStatus(id, ConversionStatus.Status.COMPLETED);
            }
            return null;
        }

        return new ConversionStatus(id, ConversionStatus.Status.valueOf(workState.name()));
    }

    @Override
    public BlobHolder getConversionResult(String id, boolean cleanTransientStoreEntry) {
        String entryKey = TransientStoreWork.computeEntryKey(id);
        BlobHolder bh = TransientStoreWork.getBlobHolder(entryKey);
        if (cleanTransientStoreEntry) {
            TransientStoreWork.removeBlobHolder(entryKey);
        }
        return bh;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(MimeTypeTranslationHelper.class)) {
            return adapter.cast(translationHelper);
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void applicationStarted(ComponentContext context) {
        startGC();
    }

    protected void startGC() {
        log.debug("CasheCGTaskActivator activated starting GC thread");
        gcTask = new GCTask();
        gcThread = new Thread(gcTask, "Nuxeo-Convert-GC");
        gcThread.setDaemon(true);
        gcThread.start();
        log.debug("GC Thread started");

    }

    public void endGC() {
        if (gcTask == null) {
            return;
        }
        log.debug("Stopping GC Thread");
        gcTask.GCEnabled = false;
        gcTask = null;
        gcThread.interrupt();
        gcThread = null;
    }

}
