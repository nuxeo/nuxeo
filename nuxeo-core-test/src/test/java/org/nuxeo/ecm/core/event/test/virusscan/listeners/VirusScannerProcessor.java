/*
 * (C) Copyright 2006-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.event.test.virusscan.listeners;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.impl.AbstractLongRunningListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.test.virusscan.VirusScanConsts;
import org.nuxeo.ecm.core.event.test.virusscan.service.ScanResult;
import org.nuxeo.ecm.core.event.test.virusscan.service.ScanService;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * Async listener that will manage the real work for scanning.
 *
 * <p/>
 * To avoid long transactions, this listeners is split in 3 parts :
 * <p/>
 * <ul>
 * <li>fetch Blobs using the xpath information stored in the EventContext
 * (transactional)</li>
 * <li>call the {@link ScanService} (outside of any transaction)</li>
 * <li>update the target documents based on the result collected from the
 * {@link ScanService} (transactional)</li>
 * </ul>
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public class VirusScannerProcessor extends AbstractLongRunningListener {

    @Override
    public boolean acceptEvent(Event event) {
        if (VirusScanConsts.VIRUS_SCAN_NEEDED_EVENT.equals(event.getName())) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean handleEventPreprocessing(EventBundle events,
            Map<String, Object> data) throws ClientException {

        for (Event event : events) {
            if (VirusScanConsts.VIRUS_SCAN_NEEDED_EVENT.equals(event.getName())) {
                VirusScanEventContext vContext = VirusScanEventContext.unwrap((DocumentEventContext) event.getContext());
                DocumentModel doc = vContext.getSourceDocument();

                if (doc.isVersion() || doc.isImmutable()) {
                    continue;
                }
                String key = doc.getRepositoryName() + ":" + doc.getId();
                Map<String, Blob> blobs = (Map<String, Blob>) data.get(key);
                if (blobs == null) {
                    blobs = new HashMap<String, Blob>();
                }
                for (String path : vContext.getBlobPaths()) {
                    blobs.put(path, (Blob) doc.getPropertyValue(path));
                }
                data.put(key, blobs);
            }
        }
        if (data.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected boolean handleEventLongRunning(List<String> eventNames,
            Map<String, Object> data) throws ClientException {

        boolean doContinue = false;
        ScanService scanService = Framework.getLocalService(ScanService.class);

        for (String key : data.keySet()) {
            Map<String, Blob> blobs = (Map<String, Blob>) data.get(key);
            Map<String, ScanResult> results = new HashMap<String, ScanResult>();

            for (String path : blobs.keySet()) {
                try {
                    results.put(path, scanService.scanBlob(blobs.get(path)));
                    doContinue = true;
                } catch (Exception e) {
                    log.error("Error calling ScanService", e);
                    results.put(
                            path,
                            ScanResult.makeFailed("Error calling ScanService "
                                    + e.getMessage()));
                }
            }
            data.put(key, results);
        }

        return doContinue;
    }

    @Override
    protected void handleEventPostprocessing(EventBundle events,
            Map<String, Object> data) throws ClientException {

        for (Event event : events) {
            if (VirusScanConsts.VIRUS_SCAN_NEEDED_EVENT.equals(event.getName())) {
                VirusScanEventContext vContext = VirusScanEventContext.unwrap((DocumentEventContext) event.getContext());
                DocumentModel doc = vContext.getSourceDocument();

                String key = doc.getRepositoryName() + ":" + doc.getId();
                Map<String, ScanResult> results = (Map<String, ScanResult>) data.get(key);
                boolean failed = false;
                StringBuilder scanInfo = new StringBuilder();
                if (results != null && results.size() > 0) {
                    scanInfo = new StringBuilder();
                    for (String path : results.keySet()) {
                        ScanResult res = results.get(path);
                        if (res.isVirusDetected()) {
                            scanInfo.append("\n virus detected for blob "
                                    + path);
                        }
                        if (res.isError()) {
                            failed = true;
                        }
                    }
                }

                if (!failed) {
                    doc.setPropertyValue(VirusScanConsts.VIRUSSCAN_STATUS_PROP,
                            VirusScanConsts.VIRUSSCAN_STATUS_DONE);
                    doc.setPropertyValue(VirusScanConsts.VIRUSSCAN_OK_PROP,
                            true);
                } else {
                    doc.setPropertyValue(VirusScanConsts.VIRUSSCAN_STATUS_PROP,
                            VirusScanConsts.VIRUSSCAN_STATUS_FAILED);
                    doc.setPropertyValue(VirusScanConsts.VIRUSSCAN_OK_PROP,
                            false);
                }
                doc.setPropertyValue(VirusScanConsts.VIRUSSCAN_INFO_PROP,
                        scanInfo.toString());
                doc.putContextData(VirusScanConsts.DISABLE_VIRUSSCAN_LISTENER,
                        true);
                doc.getCoreSession().saveDocument(doc);
            }
        }

    }

}
