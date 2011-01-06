<!-- _lcid="${config.lang}" _version="12.0.4518" _dal="1" -->
<!-- _LocalBinding -->
<html dir="ltr">
<HEAD>
<META Name="GENERATOR" Content="Microsoft SharePoint">
<META HTTP-EQUIV="Content-Type" CONTENT="text/html;charset=utf-8">
<META HTTP-EQUIV="Expires" content="0">
<Title ID=onetidTitle>Listing ${parent.description} </Title>
<BASE HREF="${request.getResourcesUrl()}">
<LINK REL="stylesheet" HREF="styles/FileOpen.css">
<script src="scripts/FileOpen.js"></script>
</HEAD>

<BODY topmargin=5 leftmargin=5 scroll=no serverType=OWS doclibsList=1
    onload="checkScroll()" onresize="checkScroll()">

<!-- Banner -->
<TABLE CELLPADDING=0 CELLSPACING=0 BORDER=0 WIDTH="100%">
    <TR>
        <TD valign=top class='ms-pagetitleareaframe' style="height: 0" nowrap>
        <table class="titleTable" cellpadding=0 cellspacing=0 width=100% border="0">
            <tr>
                <td valign="top" class="ms-titlearea" style="padding-left: 4px;">
                ${parent.description}</td>
            </tr>
            <tr>
                <td height=100% valign=top class="ms-pagetitle"
                    style="padding-left: 4px;">
                <h2 class="ms-pagetitle">${title}</h2>
                </td>
            </tr>
        </table>
        </TD>
    </TR>
    <TR>
        <TD>
        <TABLE ID="FileDialogViewTable" cellpadding=2 width=100%
            style="border-collapse: collapse; cursor: default;" cellspacing=0
            border=0>
            <TR>
                <TH class="ms-vh2-nofilter" nowrap>&nbsp;</TH>
                <TH class="ms-vh2-nofilter" nowrap>Name</TH>
                <TH class="ms-vh2-nofilter" nowrap>Description</TH>
            </TR>
            <TR ID="LibrarySection">
                <TD ID=onetidPageTitle nowrap class="ms-gb" colspan="3">
                Document Libraries</TD>
            </TR>
            <#list items as item>
            <TR class="" fileattribute=${item.type}
                ID="${request.getBaseUrl()}${item.getRelativeFilePath("")}"
                onmousedown="selectrow(this)" onclick="selectrow(this)">
                <TD valign="top" class="ms-vb2" width="16"><Img width="16" height="16"
                    SRC="icons/${item.icon}" title="icon" alt="icon"></TD>
                <TD valign="top" Class="ms-vb2">${item.displayName}</TD>
                <TD valign="top" Class="ms-vb2">
                <div dir="">${item.description}</div>
                </TD>
            </TR>
            </#list>
        </TABLE>
        </TD>
    </TR>
</TABLE>
</BODY>
</HTML>