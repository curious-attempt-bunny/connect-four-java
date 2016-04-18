#!/usr/bin/env ruby

require 'open3'
require './common'

#JAVA_BOT = "java -cp /Users/home/IdeaProjects/connect-four-java/out/production/connect-four-java com.hitechbunny.Main"
JAVA_BOT = "java -cp bot-montecarlo-v11.jar com.hitechbunny.Main"
JAVA_OTHER_BOT = "java -cp /Users/home/IdeaProjects/connect-four-java/out/production/connect-four-java com.hitechbunny.Main"

wins = {1 => 0, 2 => 0}
RUNS=25
1.upto(RUNS) do |run|
Open3.popen2(JAVA_BOT) do |o1,i1,t1|
# Open3.popen3(JAVA_BOT) do |o1,i1,e1,t1|
  # Open3.popen3(JAVA_OTHER_BOT) do |o2,i2,e2,t2|
  Open3.popen2(JAVA_OTHER_BOT) do |o2,i2,t2|
    state = "0,0,0,0,0,0,0;0,0,0,0,0,0,0;0,0,0,0,0,0,0;0,0,0,0,0,0,0;0,0,0,0,0,0,0;0,0,0,0,0,0,0"

    output = {1 => o1, 2 => o2}
    input = {1 => i1, 2 => i2}
    standard = """settings timebank 10000
settings time_per_move 500
settings player_names player1,player2
settings field_columns 7
settings field_rows 6
"""
    output[1].puts standard
    output[1].puts "settings your_bot player1\n"
    output[1].puts "settings your_botid 1\n"
    output[1].flush
    output[2].puts standard
    output[2].puts "settings your_bot player2\n"
    output[2].puts "settings your_botid 2\n"
    output[2].flush

    player = run%2 + 1

    while(true) do
      puts display(state)
      if state.split(';')[0].split(',').detect { |i| i == '0' }.nil?
        puts "DRAW"
        wins[1] += 0.5
        wins[2] += 0.5
        break
      end

      msg = "update game field #{state}"
      # puts ">#{player}> #{msg}"
      output[1].puts "#{msg}\n"
      output[2].puts "#{msg}\n"
      msg = "action move 10000"
      # puts ">#{player}> #{msg}"
      output[player].puts "#{msg}\n"
      output[player].flush

      cmd = input[player].gets.strip
      # puts "<#{player}< #{cmd}"

      args = cmd.split(' ')
      raise "UNRECOGNIZED #{args[0]}" unless args[0] == 'place_disc'

      move = args[1].to_i

      raise "FAIL" unless move >= 0 && move <= 6

      if winning_move?(state, player, move+1)
        puts "WIN FOR player#{player}"
        wins[player] += 1
        break
      end

      state = next_state_for(state, move+1, player)

      player = 3 - player
    end

    puts "#{wins[1]} vs #{wins[2]}"
    # output[1].puts "end\n"
    # output[2].puts "end\n"

    # sleep(0.5)
  end
end
end

puts "#{wins[1]} vs #{wins[2]}"