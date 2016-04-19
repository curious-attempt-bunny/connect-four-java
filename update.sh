#!/bin/sh

ruby download-games.rb
ruby build-tree.rb > tree.txt
echo 'Scraping perfect'
ruby scrape-perfect.rb
ruby build-sequences.rb > sequences.txt
cat sequences.txt
ruby inline-sequences.rb
ruby prepare-zip.rb
git add raw
git diff src
ruby upload.rb