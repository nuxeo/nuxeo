<@extends src="base.ftl">

<@block name="left" />

<@block name="right">

  <h1>Distribution Snapshot saved successfully</h1>
  <script>
    window.setTimeout("window.location.href='${Root.path}/'", 2000)
  </script>

</@block>

</@extends>
