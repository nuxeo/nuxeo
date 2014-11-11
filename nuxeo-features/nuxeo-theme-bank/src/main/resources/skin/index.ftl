<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title></title>
</head>

<frameset rows="32, *" frameborder="no">
  <frame name="actionbar" src="${Root.getPath()}/actionbar" style="background-color: #666">

  <frameset cols="200, *" frameborder="yes" framespacing="10" border="2" >
    <frame name="navtree" src="${Root.getPath()}/navtree" style="background-color: #eee">
    <#if bank>
      <frame name="main" src="${Root.getPath()}/${bank}/view">
    <#else>
      <frame name="main" src="${Root.getPath()}/banks">
    </#if>
  </frameset>
</frameset>

</html>