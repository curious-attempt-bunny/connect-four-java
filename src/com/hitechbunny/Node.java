package com.hitechbunny;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by home on 2/1/16.
 */
public class Node {
    public static Map<String,Node> table = new HashMap<>(100000);
    private static final int MAJOR_ROLLOUTS = 500; //20000; //500;
    private static final int MINOR_ROLLOUTS = 25; // 20000; //25;

    Node[] children;
    private double score;
    private String state;
    private int bot_id;
    private int multiplier;
    public long rollouts;
    private String name;
    private boolean terminal;

    public Node(String state, int bot_id, int multiplier) {
        this.state = state;
        this.bot_id = bot_id;
        this.multiplier = multiplier;
        name = "";
    }

    public List<Node> select() {
        List<Node> nodes = new ArrayList<>();
        nodes.add(this);
        return select(this.rollouts, nodes);
    }

    private List<Node> select(long rollouts, List<Node> nodes) {

        if (children == null || terminal) {
//            System.err.println("Selected "+state+" ("+multiplier+")");
            return nodes;
        }
//        System.err.println("Selecting children of "+state);

        Node best = null;
        double best_weight = 0;
//        double c = Math.sqrt(2);
        double c = Math.sqrt(10);
        double lnt = Math.log(rollouts);
        for(Node child : children) {
            if (child == null) {
                continue;
            }
//            best = child; if (1==1) break;
            double weight = (child.score+1)/2 + c*Math.sqrt(lnt / child.rollouts);
//            System.err.println("Child "+child.state+" has score "+child.score+" and weight "+weight);
//            if (best == null || (weight > best_weight && best.multiplier*best.score < 1.0) || (child.terminal && child.multiplier*child.score == 1.0)) {
            if (best == null || weight > best_weight) {
                best = child;
                best_weight = weight;
            } else if (child.terminal) {
//                System.err.println("Passed over terminal ("+weight+" ["+child.score+"] vs "+best_weight+" ["+best.score+"])");
            }
        }

        nodes.add(0, best);

        if (best.rollouts < MAJOR_ROLLOUTS) {
            return nodes;
        }

        return best.select(rollouts, nodes);
    }

    public void expand(List<Node> nodes) {
        if (terminal) {
//            System.err.println("TERMINAL!");
            rollouts += MAJOR_ROLLOUTS;
            double child_score = score;
            for(Node prev : nodes) {
                prev.add(child_score, MAJOR_ROLLOUTS);
                child_score = -child_score;
            }
            return;
        }

        if (rollouts < MAJOR_ROLLOUTS && !name.isEmpty()) {
            int[][] grid = Main.getGrid(state);
            double child_score = Main.scoreGridParallel(3 - bot_id, MAJOR_ROLLOUTS, grid);
//            child_score = child_score * multiplier;
            for(Node prev : nodes) {
                prev.add(child_score, MAJOR_ROLLOUTS);
                child_score = -child_score;
            }
            return;
        }

//        System.err.println("Expanding "+state+" (children? "+(children != null)+")");
        children = new Node[7];
        int[][] grid = Main.getGrid(state);
        double sum = 0;
        int rolls = 0;

        RolloutState rolloutState = new RolloutState();
        Main.populate_selection_options(bot_id, grid, new int[7], new int[7], rolloutState, bot_id);

        if (rolloutState.selectionOptionCount == 0) throw new RuntimeException("No legal moves for "+state);

        for(int i=0; i<rolloutState.selectionOptionCount; i++) {
            int move = rolloutState.selectionOptions[i];

            int drop = Main.getDrop(grid[move]);
            if (drop == -1) {
                // illegal move
                continue;
            }
            grid[move][drop] = bot_id;

            String nextState = Main.state_for_grid(grid);
            if (children[8-move-1] != null && children[8-move-1].state.equals(nextState)) {
                grid[move][drop] = 0;
                continue;
            }
//            System.err.println("\t"+nextState);
            Node child = find_or_create(nextState, 3 - bot_id, -multiplier);
            boolean existed = child.rollouts > 0;
            child.name = name+move;
            children[move-1] = child;
            if (!existed) {
                child.rollouts = MINOR_ROLLOUTS;

                if (Main.winning_move(grid, move, drop, bot_id)) {
                    //                if (1==1) throw new RuntimeException("winning move!");
//                    System.err.println(move + " @ " + state + " is winning move for " + bot_id);
                    child.score = 1;
                    child.terminal = true;
                } else if (grid[1][1] != 0 && grid[2][1] != 0 && grid[3][1] != 0 && grid[4][1] != 0 && grid[5][1] != 0 && grid[6][1] != 0 && grid[7][1] != 0) {
                    child.score = 0;
                    child.terminal = true;
                } else {
                    child.score = Main.scoreGridParallel(bot_id, MINOR_ROLLOUTS, grid);
                }
                if (child.score > 1 || child.score < -1) throw new RuntimeException("Distortion! " + child.score);
//                child.score = child.score * child.multiplier;
//                if (child.name.startsWith("6")) System.err.println(child.name+" scores "+child.score);
            }

            sum += child.score*child.rollouts;
            rolls += child.rollouts;

//            System.err.println(child.state+" scores "+child.score+" ("+child.multiplier+")");

            grid[move][drop] = 0;
        }

        double child_score = sum/rolls;
        for(Node prev : nodes) {
            child_score = -child_score;
            prev.add(child_score, rolls);
        }
//            if (child.terminal)

    }

    private void add(double child_score, int rolls) {
        double s = score;
        score = ((score*rollouts)+(child_score*rolls))/(rollouts+rolls);
        rollouts += rolls;
//        if (name.startsWith("6")) System.err.println(name+" adjusted to "+score+" from "+s+" (child score "+child_score+")");
    }

    public int getBestMove() {
        int bestMove = -1;
        long bestRollouts = 0;
        double bestScore = 0;

        double sum = 0;
        int rolls = 0;

        for(int i=0; i<children.length; i++) {
            if (children[i] != null) {
                sum += children[i].score*children[i].rollouts;
                rolls += children[i].rollouts;
                System.err.println(">> "+children[i].state+" has score "+children[i].score+" and rollouts "+children[i].rollouts);
                if (bestMove == -1 || children[i].rollouts > bestRollouts || (children[i].rollouts == bestRollouts && children[i].score > bestScore)) {
                    bestMove = i+1;
                    bestRollouts = children[i].rollouts;
                    bestScore = children[i].score;
                }
            }
        }

//        System.err.println("Root "+score+" should equal "+(sum/rolls));

        return bestMove;
    }

    public void dump(BufferedWriter out, int depth) throws IOException {
        out.append("  \""+state+"\" [label=\""+name+" "+rollouts+" @ "+String.format("%.3g",score)+"\"];\n");
        if (children != null) {
            for(Node child : children) {
                if (child != null && depth >= 1) {
                    child.dump(out, depth - 1);
                    out.append("  \"" + state + "\" -> \"" + child.state + "\";\n");
                }
            }
        }
    }

    public void dumpBest(BufferedWriter out, int depth) throws IOException {
        out.append("  \""+state+"\" [label=\""+name+" "+rollouts+" @ "+String.format("%.3g",score)+"\"];\n");
        if (children != null) {
            for(Node child : children) {
                if (child != null && depth >= 1) {
//                    child.dump(out, depth-1);
                    out.append("  \"" + state + "\" -> \"" + child.state + "\";\n");
                    out.append("  \""+child.state+"\" [label=\""+child.name+" "+child.rollouts+" @ "+String.format("%.3g",child.score)+"\"];\n");
                }
            }

            int move = getBestMove();
            if (move != -1) {
                children[move-1].dumpBest(out, depth - 1);
            }
        } else {
            System.err.println("Best is "+name+" with result "+score*multiplier);
        }
    }

    public static Node find_or_create(String state, int bot_id, int multiplier) {
        Node child = table.get(state);
        if (child == null) {
            child = new Node(state, bot_id, multiplier);
            table.put(state, child);
        }
        return child;
    }

    public void record(BufferedWriter out) throws IOException {
        out.append("  parent = Node.find_or_create(\""+state+"\","+bot_id+","+multiplier+");\n");
        out.append("  parent.score = "+score+";\n");
        out.append("  parent.rollouts = "+rollouts+"L;\n");
        out.append("  parent.terminal = "+terminal+";\n");
        out.append("  parent.name = \""+name+"\";\n");
        if (children != null) {
            out.append("  parent.children = new Node[] {\n");
            boolean first = true;
            for(Node child : children) {
                if (!first) {
                    out.append(",\n");
                }
                first = false;
                if (child == null) {
                    out.append("    null");
                } else {
                    out.append("    Node.find_or_create(\""+child.state+"\","+child.bot_id+","+child.multiplier+")");
                }
            }
            out.append("\n");
            out.append("  };\n");
            for(Node child : children) {
                if (child != null) {
                    child.record(out);
                }
            }
        }

    }
}
