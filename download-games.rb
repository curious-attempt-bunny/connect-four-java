#!/usr/bin/env ruby

require 'set'
require 'json'

leaderboard = `curl http://theaigames.com/competitions/four-in-a-row/leaderboard/global/a/`
leaders = Set.new(leaderboard.scan(/(?mi)<td class="cell-table cell-table-pointRight"><div class="bot-name">(.*?)<\/div>/)[0...15].map(&:first).map(&:strip))
points = Set.new(leaderboard.scan(/(?mi)<td class="cell-table cell-table-square"><em>([0-9]+)<\/em>/)[0...15].map(&:first).map(&:strip)).map(&:to_i)
leader_points = Hash[leaders.zip(points)]

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
            content = `curl #{url}`
            json = JSON.parse(content)
            json["meta"] = {
                players: players,
                scores: players.map { |p| leader_points[p] }
            }
            content = JSON.generate(json)
            File.write("raw/#{game}.json", content)
        end
    end
    break if Dir.glob('raw/*.json').size >= 5000
end