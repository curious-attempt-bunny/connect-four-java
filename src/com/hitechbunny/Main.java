package com.hitechbunny;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Main {

    // bank = 10000 ; ratio = 0.2 ; moves = 25 ; portions = bank*((1-ratio)**moves) / moves ; 1.upto(moves) { |r| print "#{(portions + bank*ratio).to_i}," ; bank -= (bank*ratio).to_i; } ; puts
    public static final int[] BONUS_TIMES = new int[] { 0,0,2001,1601,1281,1025,820,656,525,421,337,270,216,173,139,111,89,72,58,47,38,30,25,20,16,13,11 };
    public static final boolean USE_TABLE = Boolean.parseBoolean(System.getProperty("use_table", "false"));
    public static final boolean WRITE_TABLE = Boolean.parseBoolean(System.getProperty("write_table", "true"));

    private static final Map<String, Rollout> table = new HashMap<>(1000000);

    private static String key_for_move(String state, int our_bot_id, int move) {
        int[][] grid = getGrid(state);
        int drop = getDrop(grid[move]);
        if (drop == -1) {
            throw new RuntimeException("Illegal move!");
        }
        grid[move][drop] = our_bot_id;

        StringBuffer key = new StringBuffer(7*6);
        for (int j = 1; j <= 6; j++) {
            for (int i = 1; i <= 7; i++) {
                key.append(Integer.valueOf(grid[i][j]));
            }
        }

        return key.toString();
    }

    public static double score_move(String state, int our_bot_id, final int move, int rollouts) {
        int[][] grid = getGrid(state);
        int j;

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
        int[] legal = new int[7];
        int[] avoided = new int[7];
        Random r = new Random();

        int wins = 0;
        int losses = 0;
        int draws = 0;

        for(int rollout = 0; rollout< rollouts; rollout++) {
            for (int i = 0; i < 9; i++) {
                for (j = 0; j < 8; j++) {
                    s[i][j] = grid[i][j];
                }
            }
            int bot_id = 3-our_bot_id;

            while(true) {
                int legals = 0;
                int avoideds = 0;
                boolean done = false;
                boolean skipped_losses = false;
                Integer defensive_move = null;
                Integer forcePlay = null;
                Integer avoidPlay = null;

                for (int i = 1; i <= 7; i++) {
                    drop = getDrop(s[i]);
//                    System.err.println("Considering "+i+" with drop "+drop);
                    if (drop != -1) {
                        legal[legals++] = i;

                        if (winning_move(s, i, drop, bot_id)) {
                            // we win
                            if (our_bot_id == bot_id) {
                                wins++;
                            } else {
                                losses++;
                            }
                            done = true;
                            break;
                        } else if (winning_move(s, i, drop, 3-bot_id)) {
                            // we block them winning
                            defensive_move = i;
                        } else if (drop>1 && winning_move(s, i, drop-1, 3-bot_id)) {
                            // we avoid letting them win directly
                            legals--;
                            skipped_losses = true;
//                            System.err.println("Skipped loss at "+i);
                        } else {
                            boolean canWinNext = (drop>1 && winning_move(s, i, drop-1, bot_id));
                            boolean canWinAfter = (drop>2 && winning_move(s, i, drop-2, bot_id));

                            if (canWinNext && canWinAfter) {
                                forcePlay = i;
                            } else if (canWinNext && !canWinAfter) {
                                avoidPlay = i;
                                avoided[avoideds++] = i;
                                legals--;
                            }
                        }
                    }
                }
                if (done) {
                    // a winning move was available
                    break;
                }

                int selected;

                if (legals == 0 && avoidPlay != null) {
                    // no moves we want to make, just moves that would eliminate a winning move for us
                    selected = avoided[r.nextInt(avoideds)];;
                } else {
                    if (legals == 0 && skipped_losses) {
                        // no moves we want to make, just ones that let them win next turn
                        if (our_bot_id == bot_id) {
                            losses++;
                        } else {
                            wins++;
                        }
                        done = true;
                        break;
                    }

                    if (legals == 0) {
                        // no moves at all to make
                        draws++;
                        break;
                    }

                    if (defensive_move != null) {
                        // a move to stop them from winning directly
                        selected = defensive_move;
                    } else if (forcePlay != null) {
                        // a move that forces them to lose
                        selected = forcePlay;
                    } else {
                        // just regular moves
                        selected = legal[r.nextInt(legals)];
                    }
                }

                drop = getDrop(s[selected]);
                s[selected][drop] = bot_id;

                bot_id = 3-bot_id;
            }
        }

        double sum = (double)(wins - losses)/rollouts;

//        System.err.println("Move "+move+" sum: "+sum+" wins: "+wins+" losses "+losses+" draws "+draws);

        return sum;
    }

    private static int[][] getGrid(String state) {
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
        return grid;
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

    public static int generate_move(String state, int round, int our_bot_id) throws IOException {
        int best_move = 1;
        double best_score = -100;

        double[] scores = new double[8];

        if (true) {
            long start = System.currentTimeMillis();
            int move = 1;
            int depth = 1;
            final int CHUNK = 250;

            int time_limit = 490;
            if (round <= BONUS_TIMES.length) {
                time_limit += BONUS_TIMES[round-1];
            }

            boolean calculation = false;
            int tableRollouts = 0;
            while(System.currentTimeMillis() - start < time_limit) {
                if (scores[move] != 1000 && scores[move] != -1000) { // && !(depth > 1 && (scores[move] == 1.0 || scores[move] == -1.0 || scores[move] == 0.0))) {
                    double score = score_move(state, our_bot_id, move, CHUNK);

//                    System.err.print(".");
//                    System.err.flush();
                    calculation = true;

                    if (score != -1000 && score != 1000) {
                        String key = key_for_move(state, our_bot_id, move);
                        Rollout r = table.get(key);
                        if (r == null) {
                            r = new Rollout();
                            table.put(key, r);
                        }

                        if (depth == 1) {
                            r.evaluations++;
                            tableRollouts += r.freq;
                        }

                        r.score = (r.score*r.freq + score*CHUNK) / (r.freq + CHUNK);
                        r.freq += CHUNK;
                        scores[move] = r.score;
                    } else {
                        scores[move] = score;
                    }
                }
                move++;
                if (move == 8) {
                    if (!calculation) {
                        break;
                    }
                    move = 1;
                    depth += 1;
                    calculation = false;
                }
            }
            System.err.println("Got to rollout "+(CHUNK*depth + (CHUNK*(move-1)))+" (+"+tableRollouts+") in "+(System.currentTimeMillis() - start));
//        } else {
//            for (int move = 1; move <= 7; move++) {
//                scores[move] = score_move(state, our_bot_id, move, ROLLOUTS);
//            }
        }

        for(int move=1; move<=7; move++) {
            double score = scores[move];
            System.err.print("Move "+move+" scores "+score);
            if (score > best_score) {
                best_move = move;
                best_score = score;
                System.err.println(" <-- best so far");
            } else {
                System.err.println();
            }
        }

        if (scores[best_move] == 1000 || scores[best_move] == 0 || scores[best_move] == -1 || scores[best_move] == 1.0) {
            if (USE_TABLE && WRITE_TABLE) {
                writeTable();
            }
        }

        return best_move;
    }

    public static void main(String[] args) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        if (USE_TABLE) {
            readTable();
        }

        int our_bot_id = 1;
        String state = null;
        int round = 0;

        while(true) {
            String line = in.readLine();
            if (line == null || line.equals("end")) {
                break;
            }

            if (line.startsWith("settings your_botid ")) {
                our_bot_id = Integer.parseInt(line.split(" ")[2]);
            } else if (line.startsWith("update game field ")) {
                state = line.split(" ")[3];
            } else if (line.startsWith("action move ")) {
                round++;
                long start_time = System.currentTimeMillis();
                int move = generate_move(state, round, our_bot_id);
                System.out.println("place_disc "+(move-1));
                System.err.println("Time: "+(System.currentTimeMillis()-start_time));
            }
        }

        if (USE_TABLE && WRITE_TABLE) {
            writeTable();
        }
    }

    private static void writeTable() throws IOException {
        BufferedWriter f = new BufferedWriter(new FileWriter("/Users/home/IdeaProjects/connect-four-java/table.csv"));
        for (Map.Entry<String, Rollout> entry : table.entrySet()) {
            f.append(entry.getKey()+","+entry.getValue().freq+","+entry.getValue().score+","+entry.getValue().evaluations+"\n");
        }
        f.close();
    }

    private static void readTable() throws IOException {
        try {
            BufferedReader f = new BufferedReader(new FileReader("/Users/home/IdeaProjects/connect-four-java/table.csv"));
            while (true) {
                String line = f.readLine();
                if (line == null) {
                    break;
                }
                String[] parts = line.split(",");
                Rollout r = new Rollout();
                r.freq = Integer.parseInt(parts[1]);
                r.score = Double.parseDouble(parts[2]);
                r.evaluations = Integer.parseInt(parts[3]);
                table.put(parts[0], r);
            }
            f.close();
        } catch (FileNotFoundException e) {

        }
    }

    private static class Rollout {
        int freq;
        double score;
        int evaluations;
    }
}
