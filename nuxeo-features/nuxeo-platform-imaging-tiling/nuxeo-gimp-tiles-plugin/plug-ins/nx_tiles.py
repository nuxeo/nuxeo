#!/usr/bin/python

import math
import time
from gimpfu import *

def nx_outlog(key, value):
     print("*NXGIMPLOG* " + str(key) + ":" + str(value)) 

def nx_tiles(inputFilePath, t_width, t_height, t_max, outputDir, cx=-1,cy=-1):
     print "Nuxeo Tile Plugin for Gimp started..."
     t0=time.time()
     pdb=gimp.pdb

     if (outputDir[-1]!="/") :
          outputDir+="/";

     # load image
     image = pdb.gimp_file_load(inputFilePath,"")

     # get some info about image and flattern if needed
     nx_outlog("Layers", len(image.layers))
     nx_outlog("Channels", len(image.channels))
     if (RGB == image.base_type) :
          nx_outlog("BaseType", "RGB")
     else :
          if (INDEX == image.base_type) :
               nx_outlog("BaseType", "INDEXED")
          else :
               nx_outlog("BaseType", image.base_type)
     image.merge_visible_layers(0)


     # define Tile copy selection width/height and the effective number of tiles
     oWidth=image.width
     oHeight=image.height
     nx_outlog("ImageWidth",oWidth)
     nx_outlog("ImageHeight",oHeight)
     if (oWidth > oHeight) :
          copyWidth=int(oWidth / t_max)
          if (oWidth % t_max)>0 :
               copyWidth+=1
          copyHeight = int((t_height+0.0)/t_width * copyWidth)
          ntx=t_max
          nty=int(oHeight/copyHeight)
     else :
          copyHeight=oHeight / t_max          
          if (oHeight % t_max)>0 :
               copyHeight+=1
          copyWidth = int((t_width+0.0)/t_height * copyHeight)
          nty=t_max
          ntx=int(oWidth/copyWidth)

     # TODO : if copyHeight is almost equal to t_height
     # => force zoom factor to 1

     nx_outlog("XTiles",ntx)
     nx_outlog("YTiles",nty)
     nx_outlog("ZoomFactor", (t_width + 0.0)/copyWidth)
     indexText=["<html><head>", "<style>", "img {border-width:0px;}","td {padding:0px;margin:0px}","</style>","</head><body>","<table cellSpacing=\"0\">"]

     cx_range= range(ntx)
     cy_range= range(ntx)

     if ((cx!=-1) & (cy!=-1)) :
          if (cx==0):
               cx_range = range(0,cx+2)
          else :
               cx_range = range(cx-1,cx+2)
          if (cy==0) :
               cy_range = range(0,cy+2)
          else :
               cy_range = range(cy-1,cy+2)

     # loop to create the tiles by copying selection into a new image
     for y in cy_range :
          indexText.append("<tr>")
          for x in cx_range :
               startX=x*copyWidth
               startY=y*copyHeight
                    
               pdb.gimp_selection_none(image)
               pdb.gimp_rect_select(image, startX, startY, copyWidth, copyHeight, 0, 0,0)
     
               pdb.gimp_edit_copy(image.active_layer)
     
               tileImage = pdb.gimp_edit_paste_as_new()

               if ((tileImage.width<copyWidth) | (tileImage.height<copyHeight)) :
                    pdb.gimp_image_resize(tileImage, copyWidth, copyHeight, 0, 0)

               pdb.gimp_image_scale(tileImage, t_width, t_height)
               tileFileName=outputDir + "tile" + str(x) + "-" + str(y) + ".jpg"
               
               indexText.append("<td><img src=\"" + tileFileName + "\"></td>")
               pdb.gimp_file_save(tileImage, tileImage.active_layer, tileFileName, "")
               gimp.delete(tileImage)
          indexText.append("</tr>")
     
     indexText.append("</table>")
     indexText.append("<A href=\"javascript:document.getElementsByTagName('table')[0].setAttribute('cellSpacing','1')\"> show canevas</A> | ")
     indexText.append("<A href=\"javascript:document.getElementsByTagName('table')[0].setAttribute('cellSpacing','0')\"> hide canevas</A>")
     indexText.append("</body></html>")
     index = file(outputDir + "index.html","w")
     index.writelines(indexText)
     index.close()
     gimp.delete(image)
     t1=time.time()
     nx_outlog("ProcessingTime",t1-t0)
     nx_outlog("ReturnCode","OK")

register(
        "nx_tiles",
        "cut the specified image into tiles",
        "cut the specified image into tiles",
        "Thierry Delprat",
        "Thierry Delprat",
        "2008",
        "<Toolbox>/Xtns/Nuxeo/_NXTiles...",
        "RGB*, GRAY*",
        [
                (PF_STRING, "inputFilePath", "Input File Path", "/home/tiry/photos/Apogee.jpg"),
                (PF_INT, "t_width", "Tile width", 255),
                (PF_INT, "t_height", "Tile height", 255),
                (PF_INT, "t_max", "max numbers of titles on the X or Y axis", 8),
                (PF_STRING, "outputDir", "Output Directory", "/tmp/"),
                (PF_INT, "cx", "X center tile (set -1 to generate all image)", -1),
                (PF_INT, "cy", "X center tile (set -1 to generate all image)", -1)
        ],
        [],
        nx_tiles)

main()
