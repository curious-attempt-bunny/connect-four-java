#!/bin/sh

ruby download-games.rb
ruby build-tree.rb > tree.txt
echo 'Scraping perfect'
ruby scrape-perfect.rb
ruby build-sequences.rb > sequences.txt
cat sequences.txt
ruby inline-sequences.rb
git add raw
ruby prepare-zip.rb
# git diff src
ruby upload.rb
git commit -am 'Opening tweaks'
git push origin master