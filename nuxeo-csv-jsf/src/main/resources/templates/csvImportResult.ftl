<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <title>Nuxeo</title>
  <style type="text/css">
    body { margin: 0; padding: 0; background-color: #f5f5f5; width: 100% !important; }
    body { -webkit-text-size-adjust: none; -ms-text-size-adjust: none; }
    p { margin: 10px 0 5px; }
    table td { border-collapse: collapse; }
  </style>
</head>
<body>
  <div style="margin:0; padding:0; background-color:#e9ecef;font-family:Arial,sans-serif;" marginheight="0" marginwidth="0">
    <center>
      <table cellspacing="0" cellpadding="0" border="0" align="center" width="100%" height="100%" style="background-color:#e9ecef;border-collapse:collapse; font-family:Arial,sans-serif;margin:0; padding:0; min-height:100% ! important; width:100% ! important;border:none;">
        <tbody>
          <tr>
            <td align="center" valign="top" style="border-collapse:collapse;margin:0;padding:20px;border-top:0;min-height:100%!important;width:100%!important">
              <table cellspacing="0" cellpadding="0" border="0" style="border-collapse:collapse;border:none;width:100%">
                <tbody>
                  <tr>
                    <td style="background-color:#f7f7f7;border-bottom:1px dashed #e9ecef;padding:8px 20px;">
                      <p style="font-weight:bold;font-size:15px;margin:0;color:#000;">
                      ${Runtime.getProperty('org.nuxeo.ecm.product.name')}</p>
                    </td>
                  </tr>
                  <tr>
                    <td style="background-color:#fff;padding:8px 20px;"><br/>
                      <p style="margin:0;font-size:14px;">Here is your import report of the ${csvFilename} in the <a href="${importFolderUrl}" style="color:#22aee8;text-decoration:underline;word-wrap:break-word !important;">${importFolderTitle}</a> folder, launched at ${startDate} by <a href="${userUrl}" style="color:#22aee8;text-decoration:underline;word-wrap:break-word !important;">${username}</a>.
                      </p><br/>
                      <table cellpadding="6" cellspacing="0" style="border:none;border-collapse:collapse;font-size:13px;">
                        <tbody>
                          <tr>
                            <td style="border:1px solid #eee;color:#4bbd00;font-size:13px;white-space:nowrap;width: 35%;">Lines successful</td>
                            <td style="border:1px solid #eee;color:#4bbd00;font-size:13px;">
                            ${importResult.successLineCount}/${importResult.totalLineCount}
                            </td>
                          </tr>
                          <tr>
                            <td style="border:1px solid #eee;color:#777;font-size:13px;white-space:nowrap;width: 35%;">Lines skipped</td>
                            <td style="border:1px solid #eee;color:#777;font-size:13px;">${importResult.skippedLineCount}/${importResult.totalLineCount}</td>
                          </tr>
                          <tr>
                            <td style="border:1px solid #eee;color:#f56200;;font-size:13px;white-space:nowrap;width: 35%;">Lines in error</td>
                            <td style="border:1px solid #eee;color:#f56200;;font-size:13px;">
                            ${importResult.errorLineCount}/${importResult.totalLineCount}
                            </td>
                          </tr>
                        </tbody>
                      </table><br/>
                      <p style="margin:0;font-size:14px;">Skipped and error lines:</p><br/>
                      <table cellpadding="6" cellspacing="0" style="border:none;border-collapse:collapse;font-size:13px;">
                        <tbody>
                          <#list skippedAndErrorImportLogs as importLog>
                          <tr>
                            <td style="border:1px solid #eee;font-size:13px;white-space:nowrap;width: 35%;color: <#if importLog.skipped>#777<#else>#f56200</#if>;">
                                  Line ${importLog.line}
                            </td>
                            <td style="border:1px solid #eee;font-size:13px;white-space:nowrap;color: <#if importLog.skipped>#777<#else>#f56200</#if>;">
                                <#if importLog.skipped>Skipped<#else>Error</#if>
                            </td>
                            <td style="border:1px solid #eee;font-size:13px;white-space:nowrap;">
                                ${importLog.getI18nMessage()}
                            </td>
                          </tr>
                          </#list>
                        </tbody>
                      </table><br/>
                      <p style="margin:0;">
                        <a style="color:#22aee8;text-decoration:underline;word-wrap:break-word !important;" href="${importFolderUrl}">&#187; See the list of documents in the folder</a>
                      </p>
                    </td>
                  </tr>
                  <tr>
                    <td style="background-color:#f7f7f7;border-top:1px dashed #e9ecef;text-align:center;padding:8px 20px;">
                      <div style="font-size:12px;color:#bbb;">
                      You received this notification because you ran a CSV import.</div>
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
