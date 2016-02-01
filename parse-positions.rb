#!/usr/bin/env ruby

require 'json'

table = Hash.new do |h,k|
    h[k] = {freq: 0, sum: 0}
end

freqs = 0

Dir.glob('raw/*.json').each do |file|
    raw = File.read(file)
    next if raw.strip == ''
    next if raw.include?('Unknown command')

    json = JSON.parse(raw)

    winner = json['settings']['winnerplayer']
    states = json['states'].map { |s| s['field'] }[1..-1]
    score = []
    score[1] = winner == 'none' ? 0 : (winner == 'player1' ? 1 : -1)
    score[2] = -score[1]
    states = states[0..-2]
    puts states.join("\n")
end
