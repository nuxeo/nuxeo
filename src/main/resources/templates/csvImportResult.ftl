<html>

<p>Here is the result of the import of ${csvFilename}:</p>

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

<p>Skipped and error lines:</p>

<table>
<#list importLogs as importLog>
  <tr>
    <td>Line ${importLog.line}</td>
    <td><#if importLog.skipped>SKIPPED<#else>ERROR</#if></td>
    <td>${importLog.getI18nMessage()}</td>
  </tr>
</#list>
</table>

</html>
