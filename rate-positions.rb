#!/usr/bin/env ruby

require 'json'

require 'set'

all_states = Set.new

Dir.glob('raw/*.json').each do |file|
    raw = File.read(file)
    next if raw.strip == ''
    next if raw.include?('Unknown command')

    json = JSON.parse(raw)

    states = json['states'].map { |s| s['field'] }

    states.each do |state|
        next if all_states.include?(state)

        all_states << state
    end
end

File.write('states.all.txt', all_states.to_a.join("\n"))

`java -cp out/production/connect-four-java com.hitechbunny.Main eval < states.all.txt > states.training.csv`