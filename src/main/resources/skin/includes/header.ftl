<header>

  <h2>${docShare.title}</h2>

  <detail>${Context.getMessage("easyshare.label.sharedBy")}
    <a title="email address" href="mailto:${docShare.easysharefolder.contactEmail}">${docShare.easysharefolder.contactEmail}</a>
  </detail>

  <detail>
  ${Context.getMessage("easyshare.label.date.expired")} : ${docShare.dublincore.expired?string("dd/MM/yyyy")}
  </detail>

</header>