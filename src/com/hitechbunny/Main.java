package com.hitechbunny;

import org.omg.SendingContext.RunTime;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class Main {

    // bank = 10000 ; ratio = 0.2 ; moves = 25 ; portions = bank*((1-ratio)**moves) / moves ; 1.upto(moves) { |r| print "#{(portions + bank*ratio).to_i}," ; bank -= (bank*ratio).to_i; } ; puts
    public static final int TIME_LIMIT = Integer.parseInt(System.getProperty("time_limit", "490"));
    public static final boolean USE_BONUS_TIMES = Boolean.parseBoolean(System.getProperty("bonus_times", "true"));
    public static final int[] BONUS_TIMES = new int[] { 0,2001,1601,1281,1025,820,656,525,421,337,270,216,173,139,111,89,72,58,47,38,30,25,20,16,13,11 };

    private static String key_for_move(String state, int our_bot_id, int move) {
        int[][] grid = getGrid(state);
        int drop = getDrop(grid[move]);
        if (drop == -1) {
            throw new RuntimeException("Illegal move!");
        }
        grid[move][drop] = our_bot_id;
        String key = key_for_grid(grid);
        grid[move][drop] = 0;
        return key;
    }

    private static String key_for_grid(int[][] grid) {
        StringBuffer key = new StringBuffer(7*6);
        for (int j = 1; j <= 6; j++) {
            for (int i = 1; i <= 7; i++) {
                key.append(Integer.valueOf(grid[i][j]));
            }
        }

        StringBuffer alternateKey = new StringBuffer(7*6);
        for (int j = 1; j <= 6; j++) {
            for (int i = 7; i >= 1; i--) {
                alternateKey.append(Integer.valueOf(grid[i][j]));
            }
        }

        String encoding = key.toString();
        String reverse = alternateKey.toString();
        if (reverse.compareTo(encoding) < 0) {
            return reverse;
        } else {
            return encoding;
        }
    }

    public static String state_for_grid(int[][] grid) {
        StringBuffer key = new StringBuffer(7*6);
        for (int j = 1; j <= 6; j++) {
            for (int i = 1; i <= 7; i++) {
                if (i != 1) {
                    key.append(",");
                }
                key.append(Integer.valueOf(grid[i][j]));
            }
            key.append(";");
        }

        StringBuffer alternateKey = new StringBuffer(7*6);
        for (int j = 1; j <= 6; j++) {
            for (int i = 7; i >= 1; i--) {
                if (i != 7) {
                    alternateKey.append(",");
                }
                alternateKey.append(Integer.valueOf(grid[i][j]));
            }
            alternateKey.append(";");
        }

        String encoding = key.toString();
        String reverse = alternateKey.toString();
        if (reverse.compareTo(encoding) < 0) {
            return reverse;
        } else {
            return encoding;
        }
    }

    public static String getState(int[][] grid) {
        StringBuffer state = new StringBuffer(7*6);
        for (int j = 1; j <= 6; j++) {
            for (int i = 1; i <= 7; i++) {
                if (i != 1) {
                    state.append(",");
                }
                state.append(Integer.valueOf(grid[i][j]));
            }
            state.append(";");
        }

        return state.toString();
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
            grid[move][drop] = 0;
            return 1000;
        }

        double score = scoreGrid(our_bot_id, rollouts, grid);

        grid[move][drop] = 0;
        return score;
    }

    private static final int THREADS = Runtime.getRuntime().availableProcessors();
    private static ExecutorService pool = Executors.newWorkStealingPool(THREADS);

    public static double scoreGridParallel(int bot_id, int rollouts, int[][] grid) {
        int portions = rollouts / THREADS;
        List<Future<Double>> results = new ArrayList<>();
        for(int i=0; i<THREADS; i++) {
            results.add(pool.submit(() -> scoreGrid(bot_id, portions, grid)));
        }
        double result = 0;
        for (Future<Double> future : results) {
            try {
                result += future.get() / THREADS;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    public static double scoreGrid(int our_bot_id, int rollouts, int[][] grid) {
        int j;
        int drop;
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

    public static int[][] getGrid(String state) {
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

    public static int getDrop(int[] ints) {
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

    public static boolean winning_move(int[][] grid, int i, int j, int player_id) {
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

    public static int generate_move(String state, int round, int our_bot_id) throws IOException {
        long start = System.currentTimeMillis();
        final int CHUNK = 250;

        int time_limit = TIME_LIMIT;
        if (USE_BONUS_TIMES && round <= BONUS_TIMES.length) {
            time_limit += BONUS_TIMES[round - 1];
        }

        int rolloutCount = 0;
        Map<Integer, Rollout> todo = new HashMap<>();
        Rollout[] rollouts = new Rollout[8];
        Double[] book_moves = new Double[8];
        for(int i=1; i<=7 ;i++) {
            double score = score_move(state, our_bot_id, i, 1);
            if (score == 1000) {
                return i;
            } else if (score == -1000) {
                rollouts[i] = null;
            } else {
                String key = key_for_move(state, our_bot_id, i);
                book_moves[i] = Book.table.get(key);
                if (book_moves[i] != null) {
                    if (our_bot_id == 2) {
                        book_moves[i] = -book_moves[i];
                    }
                    if (book_moves[i] == -1) {
                        book_moves[i] = null;
                    }
                }
                if (book_moves[i] != null) {
                    System.err.println("Book entry for "+key);
                }
                rollouts[i] = Table.table.get(key);
                if (rollouts[i] == null) {
//                    System.err.println("No entry for "+key);
                    rollouts[i] = new Rollout();
                    rollouts[i].freq = 1;
                    rollouts[i].rlScore = score;

                    Table.table.put(key, rollouts[i]);
                    todo.put(i, rollouts[i]);

                    rolloutCount++;
                }
                rollouts[i].peers = rollouts;
            }
        }

        double book_move_sum = 0;
        for(int i=1; i<=7; i++) {
            if (book_moves[i] != null) {
                book_move_sum += book_moves[i];
            }
        }

        Integer best_book_move = null;
        double selection = Math.random()*book_move_sum;
        for (int move = 1; move <= 7; move++) {
            if (book_moves[move] == null) {
                continue;
            }
            best_book_move = move;
            if (1+book_moves[move] >= selection) {
                break;
            }
            selection -= (1+book_moves[move]);
        }
        if (best_book_move != null) {
            return best_book_move;
        }

        Iterator<Map.Entry<Integer, Rollout>> iterator = todo.entrySet().iterator();
        while(System.currentTimeMillis() - start < time_limit && !todo.isEmpty()) {
            Map.Entry<Integer, Rollout> next;
            if (!iterator.hasNext()) {
                iterator = todo.entrySet().iterator();
            }

            next = iterator.next();
            Rollout r = next.getValue();
            double score = score_move(state, our_bot_id, next.getKey(), CHUNK);
            r.rlScore = (r.rlScore * r.freq + score * CHUNK) / (r.freq + CHUNK);
            r.freq += CHUNK;
            rolloutCount += CHUNK;
        }
        if (rolloutCount > 0) {
            System.err.println("Took " + (System.currentTimeMillis() - start)+" for "+rolloutCount+" rollouts");
        }

        int best_move = 1;
        double best_score = -100;

        for (int move = 1; move <= 7; move++) {
            if (rollouts[move] == null) {
                continue;
            }

            double score = rollouts[move].rlScore;
            if (rolloutCount > 0) {
                System.err.print("Move "+move+" scores "+score+"\n");
            }
            if (score > best_score) {
                best_move = move;
                best_score = score;
                //                System.err.println(" <-- best so far");
            } else {
                //                System.err.println();
            }
        }

        return best_move;
    }

    public static int generate_mcts_move(String state, int round, int our_bot_id) throws IOException {
        long start = System.currentTimeMillis();

        int time_limit = TIME_LIMIT;
        if (USE_BONUS_TIMES && round <= BONUS_TIMES.length) {
            time_limit += BONUS_TIMES[round - 1];
        }
        time_limit = 500;

        Node root = new Node(null, state, our_bot_id, -1);

        while(System.currentTimeMillis() - start < time_limit) {
            // select
            Node leaf = root.select();
            // expand
            leaf.expand();
        }

        BufferedWriter out= new BufferedWriter(new FileWriter("mcts.dot"));
        out.append("digraph {\n");
        out.append("  rankdir=\"LR\";\n");
        root.dump(out, 3);
        out.append("}\n");
        out.close();

        return root.getBestMove();
    }

    public static void main(String[] args) throws Exception {
        System.err.println("Cores: "+ Runtime.getRuntime().availableProcessors());
        compete();
    }

    private static void compete() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

//            readTable();

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
                int move = generate_mcts_move(state, round, our_bot_id);
                System.out.println("place_disc "+(move-1));
                System.err.println("Time: "+(System.currentTimeMillis()-start_time));
            }
        }

//            writeTable();
    }

    private static void writeTable() throws IOException {
        BufferedWriter f = new BufferedWriter(new FileWriter("/Users/home/IdeaProjects/connect-four-java/table.csv"));
        for (Map.Entry<String, Rollout> entry : Table.table.entrySet()) {
            if (entry.getValue().rlScore != null) {
//            f.append(entry.getKey()+","+entry.getValue().freq+","+entry.getValue().score+","+entry.getValue().evaluations+"\n");
                f.append(entry.getKey() +
                        "," + (entry.getValue().rlScore == null ? "" : entry.getValue().rlScore) +
                        "," + (entry.getValue().score == null ? "" : entry.getValue().score) + "\n");
            }
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
                if (!parts[1].isEmpty()) {
                    r.rlScore = Double.parseDouble(parts[1]);
                }
//                if (!parts[2].isEmpty()) {
//                    r.score = Double.parseDouble(parts[2]);
//                }
                Table.table.put(parts[0], r);
            }
            f.close();
        } catch (FileNotFoundException e) {

        }
    }
}
