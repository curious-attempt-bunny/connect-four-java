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

def valid_extent(grid, i, j)
    [
        valid_extent_dir(grid, i, j, -1, -1) + valid_extent_dir(grid, i, j,  1,  1),
        valid_extent_dir(grid, i, j, -1,  0) + valid_extent_dir(grid, i, j,  1,  0),
        valid_extent_dir(grid, i, j,  0, -1) + valid_extent_dir(grid, i, j,  0,  1),
        valid_extent_dir(grid, i, j, -1,  1) + valid_extent_dir(grid, i, j,  1, -1)
    ].max
end

def valid_extent_dir(grid, i, j, di, dj)
    return 4 if grid[i][j] == 0
    extent = 0
    1.upto(4) do |delta|
        v = grid[i+delta*di][j+delta*dj]
        break unless v == 0 || v == grid[i][j]
        extent += 1
    end
    extent
end

def actual_extent(grid, i, j)
    [
        actual_extent_dir(grid, i, j, -1, -1) + actual_extent_dir(grid, i, j,  1,  1),
        actual_extent_dir(grid, i, j, -1,  0) + actual_extent_dir(grid, i, j,  1,  0),
        actual_extent_dir(grid, i, j,  0, -1) + actual_extent_dir(grid, i, j,  0,  1),
        actual_extent_dir(grid, i, j, -1,  1) + actual_extent_dir(grid, i, j,  1, -1)
    ].max
end

def actual_extent_dir(grid, i, j, di, dj)
    extent = 0
    1.upto(4) do |delta|
        v = grid[i+delta*di][j+delta*dj]
        break unless v == grid[i][j]
        extent += 1
    end
    extent
end

def winning_move?(state, player, move)
    grid = Hash.new { |h,k| h[k] = Hash.new { |h,k| h[k] = -1 } }
    state.split(';').each_with_index do |line, j|
        cells = line.split(',').map(&:to_i)
        cells.each_with_index do |cell, i|
            grid[i+1][j+1] = cell
        end
    end

    valid = nil
    1.upto(6) do |j|
        break if grid[move][j] != 0
        valid = j
    end

    raise "Illegal move #{move}" if valid.nil?

    grid[move][valid] = player

    return actual_extent(grid, move, valid) >= 3
end

def prune(state)
    grid = Hash.new { |h,k| h[k] = Hash.new { |h,k| h[k] = -1 } }
    state.split(';').each_with_index do |line, j|
        cells = line.split(',').map(&:to_i)
        cells.each_with_index do |cell, i|
            grid[i+1][j+1] = cell
        end
    end

    # puts grid.inspect

    next_state = ""
    1.upto(6) do |j|
        1.upto(7) do |i|
            grid[i][j] = 3 unless valid_extent(grid, i, j) >= 3
            next_state += grid[i][j].to_s
            next_state += ',' unless i == 7
        end
        next_state += ';' unless j == 6
    end

    # puts "#{state} -->\n#{next_state}" unless next_state == state

    next_state
end

def display(state)
    puts state.split(';').map { |line| line.gsub(/,/,'').gsub(/0/,'.') }.join("\n")
end

def next_state_for(state, move, player)
    grid = Hash.new { |h,k| h[k] = Hash.new { |h,k| h[k] = -1 } }
    state.split(';').each_with_index do |line, j|
        cells = line.split(',').map(&:to_i)
        cells.each_with_index do |cell, i|
            grid[i+1][j+1] = cell
        end
    end

    valid = nil
    1.upto(6) do |j|
        break if grid[move][j] != 0
        valid = j
    end

    raise "Illegal move #{move}" if valid.nil?

    grid[move][valid] = player

    next_state = ""
    1.upto(6) do |j|
        1.upto(7) do |i|
            next_state += grid[i][j].to_s
            next_state += ',' unless i == 7
        end
        next_state += ';' unless j == 6
    end

    next_state
end
