<#assign archiveLocationUrl = "${serverUrlPrefix}" + "${archiveLocation}">
<html>
<body>
<div style="margin:0;padding:0;background-color:#e9ecef;font-family:Arial,sans-serif;" marginheight="0" marginwidth="0">
    <center>
        <table cellspacing="0" cellpadding="0" border="0" align="center" width="100%" height="100%"
               style="background-color:#e9ecef;border-collapse:collapse;font-family:Arial,sans-serif;margin:0;padding:0;min-height:100%!important; width:100%!important;border:none;">
            <tbody>
            <tr>
                <td align="center" valign="top"
                    style="border-collapse:collapse;margin:0;padding:20px;border-top:0;min-height:100%!important;width:100%!important">
                    <table cellspacing="0" cellpadding="0" border="0"
                           style="border-collapse:collapse;border:none;width:100%">
                        <tbody>
                        <tr>
                            <td style="background-color:#f7f7f7;border-bottom:1px dashed #e9ecef;padding:8px 20px;">
                                <p style="font-weight:bold;font-size:15px;margin:0;color:#000;">
                                    ${Runtime.getProperty('org.nuxeo.ecm.product.name')}</p>
                            </td>
                        </tr>
                        <tr>
                            <td style="background-color:#fff;padding:8px 20px;">
                                <br/>
                                <table cellpadding="6" cellspacing="0"
                                       style="border:none;border-collapse:collapse;font-size:13px;">
                                    <tbody>
                                    <p style="margin:0;font-size:13px;">
                                        Your archived file of the document ${htmlEscape(docTitle)} is now available for
                                        download.
                                    </p></br>
                                    <#if coldStorageAvailableUntil??>
                                        <p style="margin:0;font-size:13px;">
                                            Please be aware that the download will remain available until the ${coldStorageAvailableUntil}.
                                        </p></br>
                                    </#if>
                                    <tr>
                                        <td style="border:1px solid #eee;color:#888;font-size:13px;white-space:nowrap;">
                                            Document
                                        </td>
                                        <td style="border:1px solid #eee;color:#000;font-size:13px;"><a href="${docUrl}"
                                                                                                        style="color:#22aee8;text-decoration:underline;word-wrap:break-word!important;">
                                                ${htmlEscape(docTitle)}</a>
                                        </td>
                                    </tr>
                                    <#assign description = document.dublincore.description />
                                    <#if description?? && description != "" >
                                        <tr>
                                            <td style="border:1px solid #eee;color:#888;font-size:13px;white-space:nowrap;">
                                                Description
                                            </td>
                                            <td style="border:1px solid #eee;color:#000;font-size:13px;">${description}</td>
                                        </tr>
                                    </#if>
                                    <tr>
                                        <td style="border:1px solid #eee;color:#888;font-size:13px;white-space:nowrap;">
                                            Author
                                        </td>
                                        <td style="border:1px solid #eee;color:#000;font-size:13px;"><a
                                                    style="color:#22aee8;text-decoration:underline;word-wrap:break-word!important;"
                                                    href="${userUrl}">
                                                <#if principalAuthor?? && (principalAuthor.lastName!="" || principalAuthor.firstName!="")>
                                                    ${htmlEscape(principalAuthor.firstName)} ${htmlEscape(principalAuthor.lastName)}
                                                </#if>
                                                (${author})</a>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="border:1px solid #eee;color:#888;font-size:13px;white-space:nowrap;">
                                            Updated
                                        </td>
                                        <td style="border:1px solid #eee;color:#000;font-size:13px;">${dateTime?datetime?string("dd/MM/yyyy - HH:mm")}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="border:1px solid #eee;color:#888;font-size:13px;white-space:nowrap;">
                                            Created
                                        </td>
                                        <td style="border:1px solid #eee;color:#000;font-size:13px;">${docCreated?datetime?string("dd/MM/yyyy - HH:mm")}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="border:1px solid #eee;color:#888;font-size:13px;white-space:nowrap;">
                                            Location
                                        </td>
                                        <td style="border:1px solid #eee;color:#000;font-size:13px;">${docLocation}</td>
                                    </tr>
                                    <tr>
                                        <td style="border:1px solid #eee;color:#888;font-size:13px;white-space:nowrap;">
                                            State
                                        </td>
                                        <td style="border:1px solid #eee;color:#000;font-size:13px;">${docState}</td>
                                    </tr>
                                    <tr>
                                        <td style="border:1px solid #eee;color:#888;font-size:13px;white-space:nowrap;">
                                            Version
                                        </td>
                                        <td style="border:1px solid #eee;color:#000;font-size:13px;">${docVersion}</td>
                                    </tr>
                                    </tbody>
                                </table>
                                <br/>

                                <p style="margin:0">
                                    <a style="color:#22aee8;text-decoration:underline;word-wrap:break-word !important;"
                                       href="${docUrl}">&#187; Consult the document ${htmlEscape(docTitle)}</a>
                                </p>
                                <p style="margin:0;">
                                    <a style="color:#22aee8;text-decoration:underline;word-wrap:break-word !important;"
                                       href="${archiveLocationUrl}">&#187; Download the archived file</a>
                                </p>
                            </td>
                        </tr>
                        <tr>
                            <td style="background-color:#f7f7f7;border-top:1px dashed #e9ecef;text-align:center;padding:8px 20px;">
                                <div style="font-size:12px;color:#bbb;">
                                    You received this notification because you requested an archived file.
                                </div>
                            </td>
                        </tr>
                        </tbody>
                    </table>
                </td>
            </tr>
            </tbody>
        </table>
    </center>
</div>
</body>
</html>
