require 'set'

known = Set.new
`touch perfect.play.txt`
File.read("perfect.play.txt").lines.each do |line|
    moves = line.split('=')[0]
    known << moves
end

File.read("tree.txt").lines.each do |line|
    moves = line.split(',')[0]
    (1..moves.size).each do |index|
        m = moves[0...index]
        unless known.include?(m)
            meta = `curl http://connect4.gamesolver.org/solve?pos=#{m}`
            File.open('perfect.play.txt', 'a') do |f|
                f << "#{m}=#{meta}\n"
            end
            known << m
        end
    end
end

