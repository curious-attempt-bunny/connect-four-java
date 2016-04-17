#!/usr/bin/env ruby

require 'set'

leaderboard = `curl http://theaigames.com/competitions/four-in-a-row/leaderboard/global/a/`
leaders = Set.new(leaderboard.scan(/(?mi)<td class="cell-table cell-table-pointRight"><div class="bot-name">(.*?)<\/div>/)[0...15].map(&:first).map(&:strip))

1.upto(500) do |page|
    response = `curl http://theaigames.com/competitions/four-in-a-row/game-log/a/#{page}`
    response.scan(/(?mi)<div class="div-botName-gameLog">(.*?)<\/div>.*?<div class="div-botName-gameLog">(.*?)<\/div>.*?href="[^"]+\/games\/([0-9a-f]+)"/).each do |match|
        players = [match[0].strip, match[1].strip]
        next unless leaders.include?(players[0]) && leaders.include?(players[1])
        game = match[2]
        url = "http://theaigames.com/competitions/four-in-a-row/games/#{game}/data"
        puts players
        puts url
        unless File.exists?("raw/#{game}.json")
            File.write("raw/#{game}.json", `curl #{url}`)
        end
    end
    break if Dir.glob('raw/*.json').size >= 2000
end