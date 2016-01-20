package com.hitechbunny;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Random;

public class Main {

    public static final int ROLLOUTS = Integer.parseInt(System.getProperty("rollouts", "10000"));
    public static final boolean SELECTS_DEFENSIVE = Boolean.parseBoolean(System.getProperty("selects_defensive", "false"));
    public static final boolean SKIP_LOSSES = Boolean.parseBoolean(System.getProperty("skip_losses", "false"));
    public static final boolean USE_TIME_LIMIT = System.getProperty("time_limit") != null;
    public static final int TIME_LIMIT = Integer.parseInt(System.getProperty("time_limit", "450"));

    public static double score_move(String state, int our_bot_id, int move, int rollouts) {
        int[][] grid = new int[9][8];

        int j = 1;
        for(String row : state.split(";")) {
            int i = 1;
            for(String cell : row.split(",")) {
                grid[i][j] = Integer.parseInt(cell);
                i++;
            }
            j++;
        }

        for(int i = 0; i<9; i++) {
            grid[i][0] = -1;
            grid[i][7] = -1;
        }

        for(j = 0; j<6; j++) {
            grid[0][j] = -1;
            grid[8][j] = -1;
        }

        int drop = getDrop(grid[move]);
        if (drop == -1) {
            // illegal move
            return -1000;
        }
        grid[move][drop] = our_bot_id;
        if (winning_move(grid, move, drop, our_bot_id)) {
            return 1000;
        }

        int[][] s = new int[9][8];
        double sum = 0;
        int[] legal = new int[7];
        Random r = new Random();

        for(int rollout = 0; rollout< rollouts; rollout++) {
            for (int i = 0; i < 9; i++) {
                for (j = 0; j < 8; j++) {
                    s[i][j] = grid[i][j];
                }
            }
            int bot_id = 3-our_bot_id;

            while(true) {
                plays++;
                int legals = 0;
                boolean done = false;
                boolean skipped_losses = false;
                Integer defensive_move = null;

                for (int i = 1; i <= 7; i++) {
                    drop = getDrop(s[i]);
//                    System.err.println("Considering "+i+" with drop "+drop);
                    if (drop != -1) {
                        legal[legals++] = i;

                        if (winning_move(s, i, drop, bot_id)) {
                            // we win
                            if (our_bot_id == bot_id) {
                                sum += 1;
                            } else {
                                sum -= 1;
                            }
                            done = true;
                            break;
                        } else if (winning_move(s, i, drop, 3-bot_id)) {
                            // we block them winning
                            defensive_move = i;
                        } else if (SKIP_LOSSES && drop>1 && winning_move(s, i, drop-1, 3-bot_id)) {
                            // we avoid letting them win directly
                            legals--;
                            skipped_losses = true;
//                            System.err.println("Skipped loss at "+i);
                        }
                    }
                }
                if (done) {
                    break;
                }

                if (legals == 0 && skipped_losses) {
                    if (our_bot_id == bot_id) {
                        sum -= 1;
                    } else {
                        sum += 1;
                    }
                    break;
                }

                if (legals == 0) {
                    sum += 0; // draw
                    break;
                }

                if (SELECTS_DEFENSIVE && defensive_move != null) {
                    move = defensive_move;
//                    System.err.println("Defensive move: "+move);
                } else {
                    move = legal[r.nextInt(legals)];
//                    System.err.println("Normal move: "+move);
                }

                drop = getDrop(s[move]);
//                System.err.println("Drop for "+move+": "+drop);
                s[move][drop] = bot_id;

                bot_id = 3-bot_id;
            }
        }

        return sum/ ROLLOUTS;
    }

    private static int getDrop(int[] ints) {
        int drop = -1;
        for(int j=1; j<=6; j++) {
            if (ints[j] == 0) {
                drop = j;
            } else {
                break;
            }
        }
        return drop;
    }

    private static boolean winning_move(int[][] grid, int i, int j, int player_id) {
        if (valid_extent_dir(grid, i, j, player_id, -1, -1) + valid_extent_dir(grid, i, j, player_id,  1,  1) >= 3) {
            return true;
        }
        if (valid_extent_dir(grid, i, j, player_id, -1,  0) + valid_extent_dir(grid, i, j, player_id,  1,  0) >= 3) {
            return true;
        }
        if (valid_extent_dir(grid, i, j, player_id,  0, -1) + valid_extent_dir(grid, i, j, player_id,  0,  1) >= 3) {
            return true;
        }
        if (valid_extent_dir(grid, i, j, player_id, -1,  1) + valid_extent_dir(grid, i, j, player_id,  1, -1) >= 3) {
            return true;
        }
        return false;
    }

    private static int valid_extent_dir(int[][] grid, int i, int j, int player_id, int di, int dj) {
//        System.err.println("i "+i+" j "+j);
        int extent = 0;
        for(int ex=1; ex<=3; ex++) {
            if (grid[i+ex*di][j+ex*dj] != player_id) {
                break;
            }
            extent++;
        }
        return extent;
    }

/*
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
     */

    public static int generate_move(String state, int our_bot_id) {
        int best_move = 1;
        double best_score = -100;

        for(int move=1; move<=7; move++) {
            double score = score_move(state, our_bot_id, move, ROLLOUTS);
            System.err.print("Move "+move+" scores "+score);
            if (score > best_score) {
                best_move = move;
                best_score = score;
                System.err.println(" <-- best so far");
            } else {
                System.err.println();
            }
        }

        return best_move;
    }

    public static void main(String[] args) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        int our_bot_id = 1;
        String state = null;

        while(true) {
            String line = in.readLine();
            if (line == null) {
                break;
            }

            if (line.startsWith("settings your_botid ")) {
                our_bot_id = Integer.parseInt(line.split(" ")[2]);
            } else if (line.startsWith("update game field ")) {
                state = line.split(" ")[3];
            } else if (line.startsWith("action move ")) {
                long start_time = System.currentTimeMillis();
                int move = generate_move(state, our_bot_id);
                System.out.println("place_disc "+(move-1));
                System.err.println("Time: "+(System.currentTimeMillis()-start_time));
            }
        }
    }
}
