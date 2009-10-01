$(document).ready( function() {

  //Choix du nombre de zones
  $(".nv-layout").click(function(){
    $("#listColumns>div").removeClass("selected");
    $("#listColumns>div>button").removeClass("selected");
    $(".typeLayout").parent().removeClass("visible");
    $(".typeLayout").parent().addClass("invisible");
    $("a[box='"+$(this).attr("box")+"']").parent().removeClass("invisible").addClass("visible");
    $("button[box='"+$(this).attr("box")+"']").addClass("selected").parent().addClass("selected");
  });

  //Choix du layout
  $(".typeLayout").click(function(){
    $(".typeLayout").removeClass("selected");
    $(this).addClass("selected");
    return false
  });

  $("#openLayoutManager").click(function(){
    $("#getLayoutManager").hide();
    $("#layoutManager").slideDown();
    return false
  });

  $("#closeLinkLayoutManager").click(function(){
    $("#getLayoutManager").show();
    $("#layoutManager").slideUp();
    return false;
  });
});