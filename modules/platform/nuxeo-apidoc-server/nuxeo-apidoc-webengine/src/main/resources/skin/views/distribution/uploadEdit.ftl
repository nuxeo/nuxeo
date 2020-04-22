<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="left">
</@block>

<@block name="right">

  The distribution zip you have uploaded contains : ${snapObject.bundleIds?size} bundles. <br/><br/>

  You can edit some of the properties of the Distribution before you validate the upload :<br/>

  <form method="POST" action="${Root.path}/uploadDistribTmpValid">
  <table>
   <tr>
     <td> Name : </td>
     <td> <input type="text" name="name" value="${tmpSnap.nxdistribution.name}"/> </td>
   </tr>
   <tr>
     <td> Version : </td>
     <td> <input type="text" name="version" value="${tmpSnap.nxdistribution.version}"/> </td>
   </tr>
   <tr>
     <td> Path Segment : </td>
     <td> <input type="text" name="pathSegment" value="${tmpSnap.name}"/></td>
   </tr>
   <tr>
     <td> Title : </td>
     <td> <input type="text" name="title" value="${tmpSnap.title}"/> </td>
   </tr>
   <tr><td colspan="2">
    <input type="submit" value="Import bundles"/>
   </td></tr>
   </table>
  </form>

</@block>

</@extends>
