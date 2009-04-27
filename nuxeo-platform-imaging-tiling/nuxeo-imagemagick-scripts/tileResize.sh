stream -map rgb -storage-type char -extract 1000x1000+900+900 $1 - |\
 convert -depth 8 -size 1000x1000 -resize 255x255 rgb:- $2

