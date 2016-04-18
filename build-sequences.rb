class Lookup
    attr_reader :result, :player1_score, :player2_score

    def initialize(result, player1_score, player2_score)
        @result = result
        @player1_score = player1_score
        @player2_score = player2_score
    end
end

class Node
    attr_reader :name, :lookups

    def initialize(scores,name)
        @children = {}
        @lookups = []
        @outcomes = {0.0 => [], 0.5 => [], 1.0 => []}
        @name = name
        @scores = scores
    end

    def add(moves, lookup)
        return if moves == ''
        move = moves[0]
        moves = moves[1..-1]

        @lookups << lookup
        # puts lookup.result
        @outcomes[lookup.result] << lookup
        @children[move] = Node.new(@scores, @name + move) unless @children.has_key?(move)

        @children[move].add(moves, lookup)

    end

    def dump(player, depth)
        return if depth == 0

        display = @children.values

        winners = []
        draws = []
        losers = []

        display.each do |child|
            if @scores.has_key?(child.name)
                if @scores[child.name] == 1
                    losers << child
                elsif @scores[child.name] == 0
                    winners << child
                else
                    draws << child
                end
            end
        end

        best_selection = display
        if winners.size > 0
            best_selection = winners
        elsif draws.size > 0
            best_selection = draws
        else
            best_selection = losers
        end

        best = display[0]
        display.each do |child|
            if child.score(player) > best.score(player) || (!best_selection.include?(best) && best_selection.include?(child))
                best = child
            end
            # puts "  \"#{child.name}\" [label=\"#{child.name} #{'%.2g' % child.score(player)} #{child.lookups.size}\"];"
        end

        if (name.size % 2)+1 == player
            display = [best]
        end

        display.each do |child|
            puts "  \"#{@name}\" -> \"#{child.name}\";"
            puts "  \"#{child.name}\" [label=<<TABLE cellspacing=\"0\" border=\"0\"><TR><TD href=\"http://connect4.gamesolver.org/?pos=#{child.name}\">#{child.name} #{'%.2g' % child.score(player)} #{child.lookups.size} #{describe(player, child.name)}</TD></TR></TABLE>>]"
        end

        display.each do |child|
            child.dump(player, depth-1) if child.lookups.size > 0
        end
    end

    def score(player)
        win = @outcomes[1.0]
        draw = @outcomes[0.5]
        loss = @outcomes[0.0]

        if player == 2
            win = @outcomes[0.0]
            loss = @outcomes[1.0]
        end

        # puts "  Raw outcomes: win #{win.size}, draw: #{draw.size}, loss: #{loss.size}"

        score_win = win.map { |lookup| lookup.player2_score }.inject(0, &:+)
        score_draw = draw.map { |lookup| lookup.player2_score }.inject(0, &:+)
        score_loss = loss.map { |lookup| lookup.player1_score }.inject(0, &:+)

        total = score_win + score_draw + score_loss
        score = (1.0 * score_win + 0.5 * score_draw) / total

        # puts "  Weighted: #{score} (win: #{score_win}, draw: #{score_draw}, loss: #{score_loss}) (win: #{score_win/win.size}, draw: #{score_draw/draw.size}, loss: #{score_loss/loss.size})"

        score * s(@lookups.size.to_f) #(@lookups.size*@lookups.size / ((@lookups.size + 1.0)*(@lookups.size + 1.0)))
    end

    # def s(x) ; x*x*x*x*x*x*x/((x+1)*(x+1)*(x+1)*(x+1)*(x+1)*(x+1)*(x+1)) ; end
    def s(x); 1; end

    def describe(player, name)
        return '???' unless @scores.has_key?(name)

        score = @scores[name]
        if (player+1)%2 == name.size % 2
            score = 1-score
        end

        if score == 0
            'win'
        elsif score == 1
            'lose'
        else
            'draw'
        end
    end

    def walk(player)
        if @children.values.empty?
            puts "add(#{player}, \"#{name}\");"
            return
        end

        display = @children.values

        winners = []
        draws = []
        losers = []

        display.each do |child|
            if @scores.has_key?(child.name)
                if @scores[child.name] == 1
                    losers << child
                elsif @scores[child.name] == 0
                    winners << child
                else
                    draws << child
                end
            end
        end

        best_selection = display
        if winners.size > 0
            best_selection = winners
        elsif draws.size > 0
            best_selection = draws
        else
            best_selection = losers
        end

        best = display[0]
        display.each do |child|
            if child.score(player) > best.score(player) || (!best_selection.include?(best) && best_selection.include?(child))
                best = child
            end
            # puts "  \"#{child.name}\" [label=\"#{child.name} #{'%.2g' % child.score(player)} #{child.lookups.size}\"];"
        end

        if (name.size % 2)+1 == player
            display = [best]
        end

        display.each do |child|
            # puts "Considering #{child.name}"
            child.walk(player)
        end
    end

    def remove(moves)
        if moves.size == 1
            @children.delete(moves)
        elsif @children.has_key?(moves[0])
            @children[moves[0]].remove(moves[1..-1])
        end
    end
end

require 'json'

scores = {}
File.read("perfect.play.txt").lines.each do |line|
    moves,json = line.split('=').map(&:strip)
    next if json.empty?
    meta = JSON.parse(json)
    score = nil
    meta['score'].each do |s|
        next if s == 100
        score = s if score.nil? || s > score
    end
    if score.nil? || score == 0
        scores[moves] = 0.5
    elsif score > 0
        scores[moves] = 1
    else
        scores[moves] = 0
    end
end

tree = Node.new(scores, '')
File.read("tree.txt").lines.each do |line|
    moves, result, player1_score, player2_score = line.split(',')
    result = result.to_f
    player1_score = player1_score.to_i
    player2_score = player2_score.to_i
    lookup = Lookup.new(result, player1_score, player2_score)

    tree.add(moves, lookup)
end

tree.remove("1")
tree.walk(1)
tree.remove("41")
tree.remove("42")
tree.remove("46")
tree.remove("47")
tree.walk(2)