#!/usr/bin/env ruby

chunk = 0
File.read('table.csv').lines.each_slice(1000) do |lines|
    puts "private static void addChunk#{chunk}() {"
    lines.each do |line|
        parts = line.strip.split(',')
        puts "add(\"#{parts[0]}\",#{parts[1]});"
    end
    puts "}"
    chunk += 1
end

puts "static {"
0.upto(chunk-1) do |i|
    puts "addChunk#{i}();"
end
puts "}"
