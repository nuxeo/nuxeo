/*
 * (C) Copyright ${year} Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     thibaud
 */
package org.nuxeo.diff.pictures;

import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;

@Name("pictDiffActions")
@Scope(ScopeType.EVENT)
public class PicturesDiffActionsBean implements Serializable {

    private static final long serialVersionUID = -1;

    private static final Log log = LogFactory.getLog(PicturesDiffActionsBean.class);

    private static final String LAST_VERSION_PROPERTY = "lastVersion";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient DocumentsListsManager documentsListsManager;

    protected DocumentModel leftDoc;

    protected String leftPictureUrl;

    protected String leftLabel;

    protected DocumentModel rightDoc;

    protected String rightPictureUrl;

    protected String rightLabel;

    protected String errorMessage;

    @Create
    public void initialize() {
        // . . .
    }

    /*
     * We delete all the temp files created during the bean lifecycle, if any
     */
    @Destroy
    public void destroy() {

        TempFilesHandler.deleteTempFolder(getLeftDocId(), getRightDocId());
    }

    private void cleanup() {

        leftPictureUrl = "";
        leftLabel = "";

        rightPictureUrl = "";
        rightLabel = "";

        errorMessage = "";
    }

    /*
     * This is the same as prepareCompareToLastVersion("lastVersion")
     */
    public boolean prepareCompareToLastVersion() throws CommandNotAvailable,
            IOException {
        return prepareCompareVersion(null);
    }

    public boolean prepareCompareVersion(String inVersionLabel)
            throws CommandNotAvailable, IOException {

        cleanup();

        rightDoc = navigationContext.getCurrentDocument();
        rightLabel = "Current";

        if (StringUtils.isBlank(inVersionLabel)
                || LAST_VERSION_PROPERTY.equals(inVersionLabel)) {
            leftDoc = documentManager.getLastDocumentVersion(rightDoc.getRef());
            if (leftDoc == null) {
                errorMessage = "Unable to diff, current document does not have any versions yet.";
                log.info(errorMessage);
                return false;
            }
        } else {
            VersionModel versionModel = new VersionModelImpl();
            versionModel.setLabel(inVersionLabel);
            leftDoc = documentManager.getDocumentWithVersion(rightDoc.getRef(),
                    versionModel);
            if (leftDoc == null) {
                errorMessage = "Unable to find version " + inVersionLabel
                        + " on current document to diff.";
                log.info(errorMessage);
                return false;
            }
        }

        leftLabel = "Version " + leftDoc.getVersionLabel();

        String lastModification = ""
                + (((Calendar) rightDoc.getPropertyValue("dc:modified")).getTimeInMillis());
        leftPictureUrl = "/nuxeo/nxpicsfile/default/" + leftDoc.getId()
                + "/Medium:content/" + lastModification;
        rightPictureUrl = "/nuxeo/nxpicsfile/default/" + rightDoc.getId()
                + "/Medium:content/" + lastModification;

        return true;
    }

    public String getLeftImageUrl() {

        return leftPictureUrl;
    }

    public String getLeftLabel() {

        return leftLabel;
    }

    public String getLeftDocId() {
        return leftDoc == null ? "" : leftDoc.getId();
    }

    public String getRightImageUrl() {

        return rightPictureUrl;
    }

    public String getRightLabel() {

        return rightLabel;
    }

    public String getRightDocId() {
        return rightDoc == null ? "" : rightDoc.getId();
    }

    public String getResultImageUrl() {

        return "";
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getDefaultFuzz() {
        return DiffPictures.DEFAULT_FUZZ;
    }

    public String getDefaultHighlightColor() {
        return DiffPictures.DEFAULT_HIGHLIGHT_COLOR;
    }

    public String getDefaultLowlightColor() {
        return DiffPictures.DEFAULT_LOWLIGHT_COLOR;
    }

}
