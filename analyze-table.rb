#!/usr/bin/env ruby

counts = {}
File.read('table2.csv').lines.each do |line|
    parts = line.strip.split(',')
    count = parts[0].split('').select { |c| c != '0' }.size
    counts[count] = (counts[count] || 0) + 1
    puts counts.to_a.sort_by(&:first).inspect if rand() < 0.1
end

puts counts.inspect
