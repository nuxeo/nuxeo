

jQuery(document).ready( function() {

  //Choix du nombre de zones
  jQuery(".nv-layout").click(function(){
    jQuery("#listColumns>div").removeClass("selected");
    jQuery("#listColumns>div>button").removeClass("selected");
    jQuery(".typeLayout").parent().removeClass("visible");
    jQuery(".typeLayout").parent().addClass("invisible");
    jQuery("a[box='"+jQuery(this).attr("box")+"']").parent().removeClass("invisible").addClass("visible");
    jQuery("button[box='"+jQuery(this).attr("box")+"']").addClass("selected").parent().addClass("selected");
  });

  //Choix du layout
  jQuery(".typeLayout").click(function(){
    jQuery(".typeLayout").removeClass("selected");
    jQuery(this).addClass("selected");
    return false
  });

  jQuery("#openLayoutManager").click(function(){
    jQuery("#getLayoutManager").hide();
    jQuery("#layoutManager").slideDown();
    return false
  });

  jQuery("#closeLinkLayoutManager").click(function(){
    jQuery("#getLayoutManager").show();
    jQuery("#layoutManager").slideUp();
    return false;
  });
});