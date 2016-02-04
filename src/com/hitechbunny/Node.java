package com.hitechbunny;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by home on 2/1/16.
 */
public class Node {
    private static Map<String,Node> table = new HashMap<>(100000);
    private static final int ROLLOUTS = 50;
    Node[] children;
    private double score;
    private Node parent;
    private String state;
    private int bot_id;
    private int multiplier;
    private int rollouts;
    private int bestMove;
    private String name;
    private boolean terminal;

    public Node(Node parent, String state, int bot_id, int multiplier) {
        this.parent = parent;
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

    private List<Node> select(int rollouts, List<Node> nodes) {

        if (children == null || terminal) {
            System.err.println("Selected "+state+" ("+multiplier+")");
            return nodes;
        }
//        System.err.println("Selecting children of "+state);

        Node best = null;
        double best_weight = 0;
        double c = Math.sqrt(2);
        double lnt = Math.log(rollouts);
        for(Node child : children) {
            if (child == null) {
                continue;
            }
//            best = child; if (1==1) break;
            double weight = (child.multiplier*child.score+1)/2 + c*Math.sqrt(lnt / child.rollouts);
//            System.err.println("Child "+child.state+" has score "+child.score+" and weight "+weight);
            if (best == null || (weight > best_weight && best.multiplier*best.score < 1.0) || child.multiplier*child.score == 1.0) {
                best = child;
                best_weight = weight;
            } else if (child.terminal) {
                System.err.println("Passed over terminal ("+weight+" ["+child.score+"] vs "+best_weight+" ["+best.score+"])");
            }
        }

        nodes.add(0, best);

        return best.select(rollouts, nodes);
    }

    public void expand(List<Node> nodes) {
        if (terminal) {
            System.err.println("TERMINAL!");
            rollouts += ROLLOUTS;
            double child_score = score;
            for(Node prev : nodes) {
                child_score = -child_score;
                prev.rollouts += ROLLOUTS;
//                System.err.print(prev.state+" "+prev.score+" --> "+child_score);
                prev.score = (
                        (prev.score*prev.rollouts)+
                                (child_score*ROLLOUTS)
                )/(prev.rollouts+rollouts);
//                System.err.println(" = "+prev.score);
            }
            return;
        }
//        System.err.println("Expanding "+state+" (children? "+(children != null)+")");
        children = new Node[7];
        int[][] grid = Main.getGrid(state);
        double sum = 0;
        int rolls = 0;
        for(int move=1; move<=7; move++) {
            int drop = Main.getDrop(grid[move]);
            if (drop == -1) {
                // illegal move
                continue;
            }
            grid[move][drop] = bot_id;

            String nextState = Main.state_for_grid(grid);
//            if (children[8-move-1] != null && children[8-move-1].state.equals(nextState)) {
//                grid[move][drop] = 0;
//                continue;
//            }
//            System.err.println("\t"+nextState);
            Node child = table.get(nextState);
            boolean existed = child != null;
            if (child == null) {
                child = new Node(this, nextState, 3 - bot_id, -multiplier);
                table.put(nextState, child);
            }
            child.name = name+move;
            children[move-1] = child;
            if (!existed) {
                child.rollouts = ROLLOUTS;

                if (Main.winning_move(grid, move, drop, bot_id)) {
                    //                if (1==1) throw new RuntimeException("winning move!");
                    System.err.println(move + " @ " + state + " is winning move for " + bot_id);
                    child.score = 1;
                    child.terminal = true;
                } else if (grid[1][1] != 0 && grid[2][1] != 0 && grid[3][1] != 0 && grid[4][1] != 0 && grid[5][1] != 0 && grid[6][1] != 0 && grid[7][1] != 0) {
                    child.score = 0;
                    child.terminal = true;
                } else {
                    child.score = Main.scoreGridParallel(bot_id, ROLLOUTS, grid);
                }
                if (child.score > 1 || child.score < -1) throw new RuntimeException("Distortion! " + child.score);
                child.score = child.score * child.multiplier;
            }

            sum += child.score;
            rolls += child.rollouts;

            System.err.println(child.state+" scores "+child.score+" ("+child.multiplier+")");

            grid[move][drop] = 0;
        }

        // TODO make this one single update
        double child_score = sum;
        for(Node prev : nodes) {
            child_score = -child_score;
            prev.rollouts += rolls;
//            if (child.terminal) System.err.print(prev.state + " " + prev.score + " --> " + child_score);
            prev.score = (
                    (prev.score*prev.rollouts)+
                            (child_score*rolls)
            )/(prev.rollouts+rolls);
//            if (child.terminal) System.err.println(" = "+prev.score);
        }
//            if (child.terminal)

    }

    public int getBestMove() {
        int bestMove = -1;
        int bestRollouts = 0;

        for(int i=0; i<children.length; i++) {
            if (children[i] != null) {
                System.err.println(">> "+children[i].state+" has score "+children[i].score+" and rollouts "+children[i].rollouts);
                if (bestMove == -1 || children[i].rollouts > bestRollouts) {
                    bestMove = i+1;
                    bestRollouts = children[i].rollouts;
                }
            }
        }

        return bestMove;
    }

    public void dump(BufferedWriter out, int depth) throws IOException {
        out.append("  \""+state+"\" [label=\""+name+" "+rollouts+" @ "+String.format("%.3g",score)+"\"];\n");
        if (children != null) {
            for(Node child : children) {
                if (child != null && depth >= 1) {
                    child.dump(out, depth-1);
                    out.append("  \"" + state + "\" -> \"" + child.state + "\";\n");
                }
            }
        }
    }
}
