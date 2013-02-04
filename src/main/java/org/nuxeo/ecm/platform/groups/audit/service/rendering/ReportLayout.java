package org.nuxeo.ecm.platform.groups.audit.service.rendering;

public class ReportLayout {
	protected int userHeaderHeight;
	protected int userHeaderRotation;
	protected int aclHeaderHeight;
	protected int aclHeaderRotation;
	protected double aclColumnWidth;
	protected double fileTreeColumnWidth;

	protected int defaultRowHeight;
	protected int freezePaneRowSplit;

	protected int aclHeaderCommentColSpan;
	protected int aclHeaderCommentRowSpan;

	protected int aclHeaderFontSize;

	protected int treeLineCursorRowStart;

	protected int splitPaneX;
	protected int splitPaneY;
	protected boolean printFullPath = false;

	protected String logoImageFile;
}
