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

    def initialize(name)
        @children = {}
        @lookups = []
        @outcomes = {0.0 => [], 0.5 => [], 1.0 => []}
        @name = name
    end

    def add(moves, lookup)
        return if moves == ''
        move = moves[0]
        moves = moves[1..-1]

        @lookups << lookup
        # puts lookup.result
        @outcomes[lookup.result] << lookup
        @children[move] = Node.new(@name + move) unless @children.has_key?(move)

        @children[move].add(moves, lookup)

    end

    def dump(player, depth)
        return if depth == 0

        display = @children.values

        best = display[0]
        display.each do |child|
            best = child if child.score(player) > best.score(player)
            puts "  \"#{child.name}\" [label=\"#{child.name} #{'%.2g' % child.score(player)} #{child.lookups.size}\"];"
        end

        if (name.size % 2)+1 == player
            display = [best]
        end

        # puts "#{name} --> #{(name.size % 2)+1}"

        @children.values.each do |child|
            puts "  \"#{@name}\" -> \"#{child.name}\";"
        end

        display.each do |child|
            child.dump(player, depth-1)
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

    def s(x) ; x*x*x*x*x*x*x/((x+1)*(x+1)*(x+1)*(x+1)*(x+1)*(x+1)*(x+1)) ; end
end

tree = Node.new('')
File.read("tree.txt").lines.each do |line|
    moves, result, player1_score, player2_score = line.split(',')
    result = result.to_f
    player1_score = player1_score.to_i
    player2_score = player2_score.to_i
    lookup = Lookup.new(result, player1_score, player2_score)

    tree.add(moves, lookup)
end

puts "digraph {"
puts "  rankdir=\"LR\";"
puts
tree.dump(2, 10)
puts "}"
# tree.dump(2)