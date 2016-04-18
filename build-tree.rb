require 'json'
require './common'

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

def finished?(name, states)
    return true if name.size == 6*7
    prior = states[-2]
    move = player_move(states[-2], states[-1])

    winning_move?(prior, (states.size % 2)+1, move)
end

Dir.glob('raw/*.json').each do |file|
    raw = File.read(file)
    next if raw.strip == ''
    next if raw.include?('Unknown command')

    json = JSON.parse(raw)

    winner = json['settings']['winnerplayer']
    states = json['states'].map { |s| s['field'] }

    score = winner == 'none' ? 0.5 : (winner == 'player1' ? 1 : 0)
    name = ""
    # states = states[0...7]
    states[0..-2].each_with_index do |state, i|
        break if states[i] == states[i+1]
        player = ((i+1)%2)+1

        move = player_move(states[i], states[i+1])
        name += move.to_s
    end
    # break
    #puts "addEntry(\"#{name}\",#{score},#{json['meta']['scores'][0]},#{json['meta']['scores'][1]});"
    puts "#{name},#{score},#{json['meta']['scores'][0]},#{json['meta']['scores'][1]}"
    #puts json
    #exit(0)

    # unless finished?(name, states)
    #     puts "*** Unfinished game #{file}"
    #     next
    # end    
end

#links.each do |n1, nodes|
#    nodes.each do |n2|
#        if value[n1][:freq] >= THRESHOLD && value[n2][:freq] >= THRESHOLD
#            puts "  \"#{n1}\" -> \"#{n2}\""
#        end
#    end
#end

#puts
#labels.each do |k,v|
#    if value[k][:freq] >= THRESHOLD
#        puts "\"#{k}\" [label=\"#{v} #{value[k][:freq]} @ #{"%.2f" % (value[k][:sum].to_f / value[k][:freq].to_f)}\",style=\"#{turn[k] == 1 ? 'filled' : 'solid'}\"]"
#    end
#end

#puts "}"

#File.open('src/com/hitechbunny/Book.java', 'w') do |f|
#    f << "package com.hitechbunny;\n"#
#
#    f << "import java.util.HashMap;\n"
#    f << "import java.util.Map;\n"
#
#    f << "public class Book {\n"
#    f << "  public static final Map<String, Double> table = new HashMap<>(1000);\n"
#    f << "\n"
#    f << "  static {\n"
#
#    value.to_a.sort_by(&:first).each do |i|
#        k,v = i
#        if v[:freq] >= THRESHOLD
#            f << "    table.put(\"#{k.gsub(/[,;]/,'')}\", #{v[:sum].to_f/v[:freq].to_f});\n"
#        end
#    end
#    f << "  }\n";
#    f << "}\n";
#end