<html>

<p>Import result of ${csvFilename}</p>
<p>Started at ${startDate} by ${username}</p>

<table>
  <tr>
    <td>Lines successful</td>
    <td>${importResult.successLineCount}/${importResult.totalLineCount}</td>
  </tr>
  <tr>
    <td>Lines skipped</td>
    <td>${importResult.skippedLineCount}/${importResult.totalLineCount}</td>
  </tr>
  <tr>
    <td>Lines in error</td>
    <td>${importResult.errorLineCount}/${importResult.totalLineCount}</td>
  </tr>
</table>

<#if skippedAndErrorImportLogs?has_content>
<p>Skipped and error lines:</p>

<table>
<#list skippedAndErrorImportLogs as importLog>
  <tr>
    <td>Line ${importLog.line}</td>
    <td><#if importLog.skipped>SKIPPED<#else>ERROR</#if></td>
    <td>${importLog.getI18nMessage()}</td>
  </tr>
</#list>
</table>
</#if>

</html>
