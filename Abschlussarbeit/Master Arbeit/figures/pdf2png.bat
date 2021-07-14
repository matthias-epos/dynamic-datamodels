rem pdf2png.bat
echo off
rem %1 PDF filename without extension
rem %2 density
rem %3 alpha, choose one of {on,off,remove}

del "%~1-*.png"

magick convert -compose copy -bordercolor red -border 3x3 -density %2 -alpha %3 "%~1.pdf" "%~1-%%02d.png"