<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title>${collection}</title>
    <link type="text/css" rel="stylesheet" href="${skinPath}/styles/ui.css" />
</head>
<body>

<h1>${collection}</h1>

<table cellspacing="10px" cellpadding="5px" style="width: 100%">
<tr>
    <#list images as image>
      <td style="text-align: center; vertical-align: middle; height: 100px; border-color: #999; border-style: outset; border-width: 1px  2px 2px 1px; margin: 5px">
      <div style="width: 100px">
      <img src="${Root.getPath()}/${bank}/image/${collection}/${image}"
        style="border: 1px inset #999; max-width: 100px; max-height: 100px" />
      </div>      
     </td>
    </#list>
</tr>
</table>

</body>
</html>
  
