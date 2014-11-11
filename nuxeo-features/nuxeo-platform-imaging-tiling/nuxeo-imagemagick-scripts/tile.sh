stream -map rgb -storage-type char -extract 600x400+1900+2900 $1 - |\
    convert -depth 8 -size 600x400 rgb:- $2

