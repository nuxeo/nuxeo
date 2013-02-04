package org.nuxeo.ecm.platform.groups.audit.service.rendering;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

import com.google.common.collect.Multimap;

/**
 * An excel layout builder that uses one group of columns per user, using
 * one column for each right type (read, write, etc).
 *
 * @author Martin Pernollet <mpernollet@nuxeo.com>
 */
public class AclExcelLayoutBuilder2 extends AclExcelLayoutBuilder {
	protected static int USERS_ROW = 0;
	protected static int PERMISSIONS_ROW = 1;

	private static ReportLayout defaultLayout() {
		ReportLayout layout = new ReportLayout();
		layout.userHeaderHeight = -1;
		layout.userHeaderRotation = 0;
		layout.aclHeaderHeight = -1;//fit vertically full ACL name 1800;
		layout.aclHeaderRotation = 0;

		layout.fileTreeColumnWidth = 2.5; // in number of char
		layout.aclColumnWidth = 2.5;
		layout.defaultRowHeight = 100;
		layout.splitPaneX = 500;
		layout.splitPaneY = 1500;
		layout.freezePaneRowSplit = 2;
		layout.treeLineCursorRowStart = 2;

		layout.aclHeaderCommentColSpan= 10;
		layout.aclHeaderCommentRowSpan=2;
		layout.aclHeaderFontSize = 6; // in font unit

		layout.logoImageFile = "src/main/resources/file-delete.png";

		return layout;
	}

	/* Prebuilt styles */

	protected CellStyle acceptStyle;
	protected CellStyle acceptStyleLeft;
	protected CellStyle acceptStyleRight;
	protected CellStyle denyStyle;
	protected CellStyle denyStyleLeft;
	protected CellStyle denyStyleRight;
	protected CellStyle emptyStyle;
	protected CellStyle emptyStyleLeft;
	protected CellStyle emptyStyleRight;
	protected int logoPictureId = -1;

	public AclExcelLayoutBuilder2() {
		this(defaultLayout());
	}

	public AclExcelLayoutBuilder2(ReportLayout layout) {
		super(layout);
	}

	protected void renderInit() {
		super.renderInit();
		acceptStyle = excel.getColoredCellStyle(ByteColor.GREEN);

		acceptStyleLeft = excel.getColoredCellStyle(ByteColor.GREEN);
		acceptStyleLeft.setBorderLeft(CellStyle.BORDER_THIN);
		acceptStyleLeft.setLeftBorderColor(excel.getColor(ByteColor.BLACK)
				.getIndex());

		acceptStyleRight = excel.getColoredCellStyle(ByteColor.GREEN);
		acceptStyleRight.setBorderRight(CellStyle.BORDER_THIN);
		acceptStyleRight.setRightBorderColor(excel.getColor(ByteColor.BLACK)
				.getIndex());

		denyStyle = excel.getColoredCellStyle(ByteColor.RED);
		denyStyle.setFillPattern(CellStyle.THIN_FORWARD_DIAG); // TODO: generaliser autres cellules
		denyStyle.setFillBackgroundColor(excel.getColor(ByteColor.WHITE).getIndex());

		denyStyleLeft = excel.getColoredCellStyle(ByteColor.RED);
		denyStyleLeft.setBorderLeft(CellStyle.BORDER_THIN);
		denyStyleLeft.setLeftBorderColor(excel.getColor(ByteColor.BLACK)
				.getIndex());

		denyStyleRight = excel.getColoredCellStyle(ByteColor.RED);
		denyStyleRight.setBorderRight(CellStyle.BORDER_THIN);
		denyStyleRight.setRightBorderColor(excel.getColor(ByteColor.BLACK)
				.getIndex());

		emptyStyle = excel.getColoredCellStyle(ByteColor.WHITE);

		emptyStyleLeft = excel.getColoredCellStyle(ByteColor.WHITE);
		emptyStyleLeft.setBorderLeft(CellStyle.BORDER_THIN);
		emptyStyleLeft.setLeftBorderColor(excel.getColor(ByteColor.BLACK)
				.getIndex());

		emptyStyleRight = excel.getColoredCellStyle(ByteColor.WHITE);
		emptyStyleRight.setBorderRight(CellStyle.BORDER_THIN);
		emptyStyleRight.setRightBorderColor(excel.getColor(ByteColor.BLACK)
				.getIndex());

		//http://www.iconarchive.com/show/soft-scraps-icons-by-deleket/File-Delete-icon.html
		//http://www.iconarchive.com/show/soft-scraps-icons-by-deleket/Document-Preview-icon.html
		//http://www.iconarchive.com/show/junior-icons-by-treetog/document-application-icon.html
		//http://www.iconarchive.com/show/junior-icons-by-treetog/document-archive-icon.html
		//http://www.iconarchive.com/show/junior-icons-by-treetog/document-preferences-icon.html
		//http://www.iconarchive.com/show/junior-icons-by-treetog/document-zip-icon.html
		//http://www.iconarchive.com/show/heaven-and-hell-icons-by-mat-u/Heaven-Documents-icon.html
		//http://www.iconarchive.com/show/pretty-office-9-icons-by-custom-icon-design/file-info-icon.html
		//http://www.iconarchive.com/show/pretty-office-9-icons-by-custom-icon-design/file-warning-icon.html
		//http://www.iconarchive.com/show/pretty-office-9-icons-by-custom-icon-design/file-complete-icon.html
		if(layout.logoImageFile!=null)
		try {
			logoPictureId = excel.loadPicture(layout.logoImageFile);
		} catch (IOException e) {
			log.error(e,e);
		}
	}

	public void renderFinal() {
		super.renderFinal();
	}

	/* HEADER RENDERING */

	/**
	 * Write users and groups on the first row. Memorize the user (or group)
	 * column which can later be retrieved with getColumn(user)
	 */
	protected void renderHeader(int tableStartColumn, Set<String> userOrGroups,
			Set<String> permissions) {
		renderHeaderPicture();
		renderHeaderUsers(tableStartColumn, userOrGroups, permissions);
		renderHeaderAcl(userOrGroups, permissions);
		formatHeaderRowHeight();
	}

	protected void renderHeaderPicture() {
		//excel.mergeRange(USERS_ROW, 0, PERMISSIONS_ROW, tableStartColumn-1);
		excel.setPicture(logoPictureId, 0, 0, false);
	}

	protected void renderHeaderUsers(int tableStartColumn,
			Set<String> userOrGroups, Set<String> permissions) {

		int userColumn = tableStartColumn;
		for (String user : userOrGroups) {
			// render the user column header
			excel.setCell(USERS_ROW, userColumn, user, userHeaderStyle);
			setUserColumn(userColumn, user);
			excel.mergeRange(USERS_ROW, userColumn, USERS_ROW, userColumn
					+ permissions.size() - 1);
			userColumn += permissions.size();
			log.debug("user header: " + USERS_ROW + "," + userColumn + " > "
					+ user);
		}
	}

	protected void renderHeaderAcl(Set<String> userOrGroups,
			Set<String> permissions) {
		for (String user : userOrGroups) {
			// render ACL column header for this user
			int userColumn;
			int aclColumn = 0;
			int aclHeaderColumn = 0;
			String aclHeaderText;
			String aclHeaderShort;

			for (String permission : permissions) {
				userColumn = getUserColumn(user);
				aclHeaderColumn = userColumn + aclColumn;
				aclHeaderText = permission;// formatPermission(permission);
				aclHeaderShort = formatPermission(permission);

				Cell cell = excel.setCell(PERMISSIONS_ROW, aclHeaderColumn, aclHeaderShort,
						aclHeaderStyle);
				excel.setColumnWidth(aclHeaderColumn,
						(int) (layout.aclColumnWidth * CELL_WIDTH_UNIT));
				excel.addComment(cell, aclHeaderText, PERMISSIONS_ROW, aclHeaderColumn, layout.aclHeaderCommentColSpan,
						layout.aclHeaderCommentRowSpan);

				setUserAclColumn(aclHeaderColumn, Pair.of(user, permission));
				aclColumn++;

				log.debug("permission header: " + PERMISSIONS_ROW + ","
						+ aclHeaderColumn + " > "
						+ formatPermission(permission));
			}
		}
	}

	protected void formatHeaderRowHeight() {
		if (layout.aclHeaderHeight != -1)
			excel.setRowHeight(PERMISSIONS_ROW, layout.aclHeaderHeight);
		if (layout.userHeaderHeight != -1)
			excel.setRowHeight(USERS_ROW, layout.userHeaderHeight);
	}

	/* FILE TREE AND MATRIX CONTENT RENDERING */

	protected void renderAcl(DocumentModel doc) throws ClientException {
		Multimap<String, Pair<String, Boolean>> userAcls = acl
				.getAclByUser(doc);
		for (String user : userAcls.keySet()) {
			List<Pair<String, Boolean>> acls = new ArrayList<Pair<String, Boolean>>(
					userAcls.get(user));
			int last = acls.size() - 1;

			// TODO: IF ACLS not contain an ACL that should be first or last,
			// thus showing border, post draw white cells

			for (int i = 0; i < acls.size(); i++) {
				boolean isFirst = false;// (i == 0);
				boolean isLast = false;// (i == last);
				Pair<String, Boolean> ace = acls.get(i);
				String permission = ace.a;
				boolean accept = ace.b;
				int aclColumn = getUserAclColumn(Pair.of(user, permission));
				String aceText = "";// formatAce(ace)

				if (accept) {
					// draws an accept cell
					renderAcceptCell(isFirst, isLast, aclColumn, aceText);
				} else {
					// draws a deny cell
					renderDenyCell(isFirst, isLast, aclColumn, aceText);
				}
			}
			// String info = formatAcl(userAcls.get(user));

		}
	}

	/** Render a cell with a 'deny' color with left, right or no border according to its position.*/
	protected void renderDenyCell(boolean isFirst, boolean isLast, int aclColumn,
			String aceText) {
		if (isFirst) {
			excel.setCell(treeLineCursor, aclColumn, aceText, denyStyleLeft);
		} else if (isLast) {
			excel.setCell(treeLineCursor, aclColumn, aceText, denyStyleRight);
		} else {
			excel.setCell(treeLineCursor, aclColumn, aceText, denyStyle);
		}
	}

	/** Render a cell with a 'accept' color with left, right or no border according to its position.*/
	protected void renderAcceptCell(boolean isFirst, boolean isLast,
			int aclColumn, String aceText) {
		if (isFirst) {
			excel.setCell(treeLineCursor, aclColumn, aceText, acceptStyleLeft);
		} else if (isLast) {
			excel.setCell(treeLineCursor, aclColumn, aceText, acceptStyleRight);
		} else {
			excel.setCell(treeLineCursor, aclColumn, aceText, acceptStyle);
		}
	}
}
