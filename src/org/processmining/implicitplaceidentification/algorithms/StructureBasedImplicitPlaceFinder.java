package org.processmining.implicitplaceidentification.algorithms;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.sf.javailp.*;
import org.processmining.implicitplaceidentification.algorithms.util.AlgebraClass;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * This class provides methods to color and remove implicit places in a petri net.
 * The finding algorithm is based on the structure of the net.
 */
public class StructureBasedImplicitPlaceFinder {
    private final Petrinet petrinet;
    /**
     * initial marking as vector
     */
    private final int[] m0;
    private final HashSet<Integer> foundImplicitPlaces = new HashSet<>();

    private final BiMap<Place, Integer> placeToRowMap;
    private final BiMap<Transition, Integer> transitionToColumnMap;
    private int[][] pre;
    private int[][] post;
    private int[][] c;

    private FindMode findMode;

    public StructureBasedImplicitPlaceFinder(Petrinet petrinet, Marking initialMarking, FindMode findMode) {
        this.petrinet = petrinet;
        this.findMode = findMode;

        // build maps to seemlessly switch between matrix indexes and places/transitions
        placeToRowMap = HashBiMap.create();
        transitionToColumnMap = HashBiMap.create();

        Integer placeIndex = 0;
        for (Place p : this.petrinet.getPlaces()) {
            placeToRowMap.put(p, placeIndex);
            placeIndex++;
        }

        Integer transitionIndex = 0;
        for (Transition t : this.petrinet.getTransitions()) {
            transitionToColumnMap.put(t, transitionIndex);
            transitionIndex++;
        }

        // compute matrices
        computeMatrices();

        // transform initial marking to marking vector
        m0 = new int[placeToRowMap.size()];
        initialMarking.forEach(p -> m0[placeToRowMap.get(p)]++);
    }

    public void setFindMode(FindMode findMode) {
        this.findMode = findMode;
    }

    public Set<Place> find() {

        foundImplicitPlaces.clear();

        /*
        --------------------CONSOLE DEBUGGING STUFF START ------------------
         */
        String transitionLabels = "   ";
        for (Object i : Arrays.stream(transitionToColumnMap.values().toArray()).sorted().toArray()) {
            transitionLabels += transitionToColumnMap.inverse().get(i).getLabel();
        }
        System.out.println(transitionLabels);
        // console debugging stuff
        printMatrix(pre, "Pre");
        printMatrix(post, "Post");
        printMatrix(c, "C");

        /*
        --------------------CONSOLE DEBUGGING STUFF END ------------------
         */

        findAndMarkDuplicatePlaces();

        for (Place p : placeToRowMap.keySet()) {
            Set<Place> placesThatMakePImplicit = getPlacesImplyingP(p);
            if (!placesThatMakePImplicit.isEmpty()) {
                onImplicitPlaceFinding(placeToRowMap.get(p));
            }
        }
        return foundImplicitPlaces.stream().map(id -> placeToRowMap.inverse().get(id)).collect(Collectors.toSet());
    }

    public Set<Place> getPlacesImplyingP(Place place) {
        int p = placeToRowMap.get(place);
        Result result = solveIlp(m0, pre, c, p);

        HashSet<Place> placesThatMakePimplicit = new HashSet<>();

        if (result != null && m0[p] >= Math.max(0L, (Long) result.getObjective()) && petrinet.getOutEdges(placeToRowMap.inverse().get(p)).size() > 0) {
            System.out.println("Place " + placeToRowMap.inverse().get(p).getLabel() + " with y: " + result);

            for (int i = 0; i < placeToRowMap.size(); i++) {
                Number resultForPlace = result.get("y" + i);
                if (resultForPlace.floatValue() >= 1) {
                    placesThatMakePimplicit.add(placeToRowMap.inverse().get(i));
                }
            }

        } else if (result != null) {
            System.out.println(placeToRowMap.inverse().get(p).getLabel() + "is NOT implicit with the ilp result:" + result);
        }
        return placesThatMakePimplicit;
    }

    /**
     * adds the place to the set of found implicit places and depending on the {@link FindMode} removes the place
     * from equations.
     *
     * @param p index of the found IP
     */
    private void onImplicitPlaceFinding(int p) {
        switch (findMode) {
            case GREEDY:
                Arrays.fill(pre[p], 0);
                Arrays.fill(post[p], 0);
                Arrays.fill(c[p], 0);
                m0[p] = 0;
                break;
            case FIND_ALL_POTENTIAL_IPS:
            default:
        }

        foundImplicitPlaces.add(p);
    }

    public Set<Place> findStructurallyImplicitPlaces() {

        foundImplicitPlaces.clear();

        for (int p = 0; p < placeToRowMap.size(); p++) {
            Result result = solveIlpForOnlyStructuralImplicitness(c, p);

            if (result != null) {
                System.out.println("Place " + placeToRowMap.inverse().get(p).getLabel() + " with y: " + result);
                onImplicitPlaceFinding(p);
            }
        }

        return foundImplicitPlaces.stream().map(id -> placeToRowMap.inverse().get(id)).collect(Collectors.toSet());
    }

    /**
     * preprocessing
     */
    private void findAndMarkDuplicatePlaces() {//TODO put in a static utility class?
        for (int i = 0; i < pre.length - 1; i++) {
            for (int j = i + 1; j < pre.length; j++) {
                if (Arrays.equals(pre[i], pre[j]) && Arrays.equals(post[i], post[j])) {
                    // the inputs (post) and the outputs (pre) of places i,j are equal
                    onImplicitPlaceFinding(j);
                    System.out.println("[PREPROCESSING] found implicit place: " + placeToRowMap.inverse().get(j));
                }
            }
        }
    }


    private static Result solveIlp(int[] m0, int[][] pre, int[][] c, int p) {
        SolverFactoryLpSolve factory = new SolverFactoryLpSolve();
        factory.setParameter(Solver.VERBOSE, 0);
        factory.setParameter(Solver.TIMEOUT, 100); // set timeout to 100 seconds

        // Problem: min (y * m0 + mu)

        Problem problem = new Problem();

        Linear linear = new Linear();

        for (int i = 0; i < m0.length; i++) {
            linear.add(m0[i], "y" + i);
        }
        linear.add(1, "mu");

        problem.setObjective(linear, OptType.MIN);

        // constraint: y^T * C <= C[p,T]

        for (int column = 0; column < c[0].length; column++) {
            linear = new Linear();
            for (int row = 0; row < c.length; row++) {
                linear.add(c[row][column], "y" + row);
            }
            problem.add(linear, "<=", c[p][column]);
        }

        // constraint: y^T*Pre[P,t] + mu >= Pre[p,t] for all outgoing ts of p
        linear = new Linear();
        Stack<Integer> outgoingTransitions = new Stack<>();
        for (int i = 0; i < pre[p].length; i++) {
            if (pre[p][i] > 0) {
                outgoingTransitions.push(i);
            }
        }

        for (int t : outgoingTransitions) {
            for (int i = 0; i < pre.length; i++) {
                linear.add(pre[i][t], "y" + i);
            }
            problem.add(linear, ">=", pre[p][t]);
        }

        // y^T >= 0, y[p] = 0
        for (int i = 0; i < m0.length; i++) {
            linear = new Linear();
            linear.add(1, "y" + i);

            if (i == p) {
                problem.add(linear, "=", 0);
            } else {
                problem.add(linear, ">=", 0);
            }
            problem.setVarType("y" + i, Integer.class);
        }


        problem.setVarType("mu", Integer.class);

        Solver solver = factory.get();
        return solver.solve(problem);
    }

    private static Result solveIlpForOnlyStructuralImplicitness(int[][] c, int p) {
        SolverFactoryLpSolve factory = new SolverFactoryLpSolve();
        factory.setParameter(Solver.VERBOSE, 0);
        factory.setParameter(Solver.TIMEOUT, 100); // set timeout to 100 seconds

        /*
        min (y * m0 + mu)
         */

        Problem problem = new Problem();

        Linear linear = new Linear();

        for (int i = 0; i < c.length; i++) {
            linear.add(1, "y" + i);
        }

        problem.setObjective(linear);

        // constraint: y^T * C <= C[p,T]

        for (int column = 0; column < c[0].length; column++) {
            linear = new Linear();
            for (int row = 0; row < c.length; row++) {
                linear.add(c[row][column], "y" + row);
            }
            problem.add(linear, "<=", c[p][column]);
        }


        // y^T >= 0, y[p] = 0
        for (int i = 0; i < c.length; i++) {
            linear = new Linear();
            linear.add(1, "y" + i);

            if (i == p) {
                problem.add(linear, "=", 0);
            } else {
                problem.add(linear, ">=", 0);
            }
            problem.setVarType("y" + i, Integer.class);
        }

        Solver solver = factory.get();
        return solver.solve(problem);
    }

    private void computeMatrices() {
        pre = AlgebraClass.computePreIncidenceMatrix(petrinet, placeToRowMap, transitionToColumnMap);
        post = AlgebraClass.computePostIncidenceMatrix(petrinet, placeToRowMap, transitionToColumnMap);
        c = AlgebraClass.computeIncidenceMatrix(pre, post);
    }

    private void printMatrix(int[][] matrix, String label) {

        System.out.println("Printing " + label);

        String row;

        for (int i = 0; i < matrix.length; i++) {
            // length returns number of rows
            row = "";
            for (int j = 0; j < matrix[i].length; j++) {
                // here length returns # of columns corresponding to current row
                row = row + "    " + matrix[i][j];
            }
            System.out.println(placeToRowMap.inverse().get(i).getLabel() + ":" + row);
        }
        System.out.println();
    }
}
