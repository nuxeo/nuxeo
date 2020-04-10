<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
<script>
  $(function() {
    $('.image-link').magnificPopup({type:'image'});
  });
</script>
</@block>

<@block name="right">
<div class="explorer-home">
<div class="fullspace intro">
  <a class="image-link" title="Relation between Bundles, Services, Extension Points, Contributions, Operations in Nuxeo Platform" href="${skinPath}/images/platform-explorer.png">
    <img src="${skinPath}/images/platform-explorer.png">
  </a>
  <p class="main">
  Explore services, extension points, contributions, operations of the Nuxeo Platform to build your own components.
  </p>
  <p class="second">
  The Nuxeo Platform provides Java Services declared inside components. Components are declared via XML files. Services are configurable by an extension system.
  </p>
  <#if Root.showCurrentDistribution()>
  <p class="second">
  Browse the running platform or a platform that has been snapshotted and saved into local Document Repository. Snapshotted platform are stored as documents and therefore can be searchable.
  </p>
  </#if>
</div>

<div>
<h2>Available Platforms</h2>

<#assign rtSnap=Root.runtimeDistribution/>
<#assign snapList=Root.listPersistedDistributions()/>

<div class="">

  <ul class="timeline">
  <#if Root.showCurrentDistribution()>
    <li>
      <time class="time" datetime="2013-04-10 18:30">
        <span class="date">${rtSnap.creationDate?date}</span>
        <span class="sticker current">Running Platform</span>
      </time>
      <div class="timepoint"></div>
      <div class="timebox">
        <div class="box-title">
          <div>
            <a class="distrib" href="${Root.path}/current/">
              <span class="number">${rtSnap.name}</span>
              <span class="detail">${rtSnap.version}</span>
            </a>
          </div>
        </div>
        <div class="flex-ctn">
          <div>
            <a class="extensions" href="${Root.path}/current/listExtensionPoints">Contribute to an Extension</a>
          </div>
          <div><a class="contributions" href="${Root.path}/current/listContributions">Override a Contribution</a></div>
          <div><a class="operations" href="${Root.path}/current/listOperations">Search Operations</a></div>
          <div><a class="services" href="${Root.path}/current/listServices">Browse Services</a></div>
        </div>
      </div>
    </li>
  </#if>

  <#list snapList as distrib>
    <li>
      <time class="time" datetime="2013-04-10 18:30">
        <span class="date">${distrib.releaseDate?date}</span>
        <#if distrib.latestFT >
          <span class="sticker current">Latest FT</span>
        <#elseif distrib.latestLTS >
          <span class="sticker current">Latest LTS</span>
        <#else>
          &nbsp;
        </#if>
      </time>
      <div class="timepoint"></div>
      <div class="timebox">
        <div class="box-title">
          <div>
            <a class="distrib" href="${Root.path}/${distrib.key}/">
              <span class="number">${distrib.name}</span>
              <span class="detail">${distrib.version}</span>
            </a>
          </div>
        </div>
        <div class="flex-ctn">
          <div>
            <a class="extensions" href="${Root.path}/${distrib.key}/listExtensionPoints">Contribute to an Extension</a>
          </div>
          <div><a class="contributions" href="${Root.path}/${distrib.key}/listContributions">Override a Contribution</a></div>
          <div><a class="operations" href="${Root.path}/${distrib.key}/listOperations">Search Operations</a></div>
          <div><a class="services" href="${Root.path}/${distrib.key}/listServices">Browse Services</a></div>
        </div>
    </li>
  </#list>


  </ul>

</div>
</div>

<div class="fullspace intro center">
  <p>
   Thanks to the Nuxeo Platform modularity, declare your service and its extensions in a given component and contribute to this extension in an other component that might come with your customisation.</p>
  <a target="_blank" class="button" href="https://doc.nuxeo.com/x/DIAO">Read Documentation</a>
</div>


<#if Root.isEditor()>
  <div class="fullspace">
    <h2>Add your distribution</h2>
    <form class="box" method="POST" action="${Root.path}/uploadDistribTmp" enctype="multipart/form-data" >
      <p>Upload your distribution that has been exported as a zip</p>
      <input type="file" name="archive">
      <input type="submit" value="Upload">
    </form>
  </div>
</#if>
<#if !Root.isSiteMode()>
  <div class="fullspace">
    <h2>Documentation</h2>
    <p>
      Documentation items are associated to Nuxeo Platform artifacts. The documentation contains currently ${Root.documentationInfo}. <a href="${Root.path}/downloadDoc">Export all documentation as a zip.</a>
    </p>
    <#if Root.isEditor()>
      <form method="POST" action="${Root.path}/uploadDoc" enctype="multipart/form-data" class="box">
        <p>Upload a documentation pack (zip)</p>
        <input type="file" name="archive">
        <input type="submit" value="Upload doc pack">
      </form>
    </#if>
  </div>
</#if>

</div>

</@block>

</@extends>
