#!/usr/bin/env ruby

require 'json'

labels = {}
links = {}
value = {}
turn = {}

THRESHOLD = 5

puts "digraph {"
puts "  rankdir=\"LR\";"
puts

def normalize(state)
    flip = false
    state.split(';').each do |line|
        cells = line.split(',').map(&:to_i)

        delta = cells[0..2].inject(&:+) - cells[4..6].inject(&:+)
        next if delta == 0
        flip = delta < 0
        # puts "#{line} has #{delta} -> flip = #{flip}"
        break
    end

# flip = false
    return state unless flip

    flipped = state.split(';').map(&:reverse).join(';')

    # puts "#{state} -> #{flipped}"

    flipped
end

def player_move(state1, state2)
    s1 = state1.gsub(/[;,]/,'')
    s2 = state2.gsub(/[;,]/,'')
    i = nil
    s1.split('').each_with_index do |c,x|
        if s1[x] != s2[x]
            i = x
            break
        end
    end
    # puts "#{state1}\n#{state2}" if i.nil?
    (i%7)+1
end

Dir.glob('raw/*.json').each do |file|
    raw = File.read(file)
    next if raw.strip == ''
    next if raw.include?('Unknown command')

    json = JSON.parse(raw)

    winner = json['settings']['winnerplayer']
    states = json['states'].map { |s| normalize(s['field']) }
    score = []
    score[1] = winner == 'none' ? 0 : (winner == 'player1' ? 1 : -1)
    score[2] = -score[1]
    # puts "Winner: #{winner} - scores #{score[1]} / #{score[2]}"
    name = ""
    # states = states[0...7]
    states[0..-2].each_with_index do |state, i|
        break if states[i] == states[i+1]
        player = ((i+1)%2)+1

        move = player_move(states[i], states[i+1])
        links[states[i]] ||= []
        turn[states[i]] = (i%2)+1
        turn[states[i+1]] = ((i+1)%2)+1
        unless links[states[i]].include?(states[i+1])
            links[states[i]] << states[i+1]
            # puts "  \"#{state}\" -> \"#{states[i+1]}\";"
        end
        value[states[i]] ||= {sum: 0, freq: 0}
        value[states[i+1]] ||= {sum: 0, freq: 0}
        name += move.to_s
        unless name == '41'
            value[states[i+1]][:sum] += score[1]
            value[states[i+1]][:freq] += 1
        end

        labels[states[i]] = "" if i == 0
        labels[states[i+1]] = move.to_s
    end
    # break
end

links.each do |n1, nodes|
    nodes.each do |n2|
        if value[n1][:freq] >= THRESHOLD && value[n2][:freq] >= THRESHOLD
            puts "  \"#{n1}\" -> \"#{n2}\""
        end
    end
end

puts
labels.each do |k,v|
    if value[k][:freq] >= THRESHOLD
        puts "\"#{k}\" [label=\"#{v} #{value[k][:freq]} @ #{"%.2f" % (value[k][:sum].to_f / value[k][:freq].to_f)}\",style=\"#{turn[k] == 1 ? 'filled' : 'solid'}\"]"
    end
end

puts "}"

File.open('src/com/hitechbunny/Book.java', 'w') do |f|
    f << "package com.hitechbunny;\n"

    f << "import java.util.HashMap;\n"
    f << "import java.util.Map;\n"

    f << "public class Book {\n"
    f << "  public static final Map<String, Double> table = new HashMap<>(1000);\n"
    f << "\n"
    f << "  private static void add(String state, double score) {\n"
    f << "    Connect4 c4 = new Connect4();\n"
    f << "    c4.parseState(state);\n"
    f << "    table.put(c4.positioncode(), score);\n"
    f << "  }\n"
    f << "\n"
    f << "  static {\n"
    f << "\n"

    value.to_a.sort_by(&:first).each do |i|
        k,v = i
        if v[:freq] >= THRESHOLD
            f << "    table.put(\"#{k.gsub(/[,;]/,'')}\", #{v[:sum].to_f/v[:freq].to_f});\n"
        end
    end
    f << "  }\n";
    f << "}\n";
end