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
  <div style="background:#f5f5f5;">
    <table style="text-align: left;  font-size: 15px; font-family: Lucida Grande, Arial, sans-serif; color: #333; margin: 0 auto; width: 600px;" border="0" cellpadding="0" cellspacing="0" width="600">
      <tr height="40"></tr>
      <tr>
        <td>
          <table style="border: 3px solid #eee; border-radius: 8px; -moz-border-radius:8px; -webkit-border-radius:8px; padding: 20px; " bgcolor="white" border="0" cellpadding="0" cellspacing="0">
            <tbody>
            <tr>
              <td style="border-bottom: 2px dotted #f1f1f1; font-size: 18px; font-family: Lucida Grande, Arial, sans-serif; font-weight:bold; padding-bottom: 10px;">
                ${Runtime.getProperty('org.nuxeo.ecm.product.name')}
              </td>
            </tr>
            <tr height="20"></tr>
            <tr>
              <td style="font-size: 15px; font-family: Lucida Grande, Arial, sans-serif; font-weight:bold;">Hello,</td>
            </tr>
            <tr height="10"></tr>
            <tr>
              <td style="font-weight:bold; font-size:16px; line-height: 24px;">
                Here is your import report of the ${csvFilename} in the <a href="${importFolderUrl}" style="color: #4783de;">${importFolderTitle}</a> folder, launched at ${startDate} by <a href="${userUrl}" style="color: #4783de;">${username}</a>.
              </td>
            </tr>
            <tr height="10"></tr>
            <tr>
              <td>
                <table width="100%">
                  <tbody>
                  <tr>
                    <td width="35%" style="color: #4bbd00;">Lines successful</td>
                    <td style="color: #4bbd00;">${importResult.successLineCount}/${importResult.totalLineCount}</td>
                  </tr>
                  <tr>
                    <td width="35%" style="color: #777777;">Lines skipped</td>
                    <td style="color: #777777;">${importResult.skippedLineCount}/${importResult.totalLineCount}</td>
                  </tr>
                  <tr>
                    <td width="35%" style="color: #f56200;">Lines in error</td>
                    <td style="color: #f56200;">${importResult.errorLineCount}/${importResult.totalLineCount}</td>
                  </tr>
                  </tbody>
                </table>
                <p style="font-weight:bold; font-size:16px;">Skipped and error lines:</p>
                <table width="100%">
                  <tbody>
                  <#list skippedAndErrorImportLogs as importLog>
                  <tr>
                      <td style="color: <#if importLog.skipped>#777<#else>#f56200</#if>;">
                          Line ${importLog.line}
                      </td>
                      <td style="color: <#if importLog.skipped>#777<#else>#f56200</#if>;">
                        <#if importLog.skipped>Skipped<#else>Error</#if>
                      </td>
                      <td>
                        ${importLog.getI18nMessage()}
                      </td>
                  </tr>
                  </#list>
                  </tbody>
                </table>
              </td>
            </tr>
            <tr height="20"></tr>
            <tr>
                <td align="left">
                    <a href="${importFolderUrl}" style="color: #4783de">See the list of documents in the folder »</a>
                </td>
            </tr>
            <tr height="30"></tr>
            <tr>
                <td style="font-weight:bold; font-size:16px; line-height: 24px;">
                  Thanks,
                </td>
            </tr>
            <tr>
                <td style="font-weight:bold; font-size:16px; line-height: 24px;">
                  ${Runtime.getProperty('nuxeo.notification.eMailSignatory')}
                </td>
            </tr>
            </tbody>
          </table>
        </td>
      </tr>
      <tr height="90"></tr>
    </table>
  </div>
</body>
</html>
