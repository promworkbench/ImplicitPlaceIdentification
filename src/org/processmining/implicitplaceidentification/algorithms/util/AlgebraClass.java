package org.processmining.implicitplaceidentification.algorithms.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import java.util.Map;

public class AlgebraClass {

    public static int[] fireTransitionOnIncidenceMatrix(int[] m1, int[][] incidenceMatrix, int transitionIndex) {
        int[] m2 = new int[m1.length];
        int[] tokenmovement = getColumnOfMatrix(incidenceMatrix, transitionIndex);
        for (int i = 0; i < m1.length; i++) {
            m2[i] = m1[i] + tokenmovement[i];
        }
        return m2;
    }

    public static int[] getColumnOfMatrix(int[][] matrix, int index) {
        int[] column = new int[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            column[i] = matrix[i][index];
        }
        return column;
    }

    public static boolean arrayIsGreaterOrEqualTo(int[] a1, int[] a2) {
        if (a1.length != a2.length) {
            return false;
        }
        for (int i = 0; i < a1.length; i++) {
            if (!(a1[i] >= a2[i])) {
                return false;
            }
        }
        return true;
    }

    public static boolean arrayIsStrictlyGreaterThan(int[] a1, int[] a2) {
        if (a1.length != a2.length) {
            return false;
        }
        for (int i = 0; i < a1.length; i++) {
            if (!(a1[i] > a2[i])) {
                return false;
            }
        }
        return true;
    }

    public static BiMap<Place, Integer> createPlaceToIndexBiMap(Petrinet petrinet) {
        BiMap<Place, Integer> placeToRowMap = HashBiMap.create();

        Integer placeIndex = 0;
        for (Place p : petrinet.getPlaces()) {
            placeToRowMap.put(p, placeIndex);
            placeIndex++;
        }

        return placeToRowMap;
    }

    public static BiMap<Transition, Integer> createTransitionToIndexBiMap(Petrinet petrinet) {
        BiMap<Transition, Integer> transitionToColumnMap = HashBiMap.create();

        Integer transitionIndex = 0;
        for (Transition t : petrinet.getTransitions()) {
            transitionToColumnMap.put(t, transitionIndex);
            transitionIndex++;
        }
        return transitionToColumnMap;
    }

    public static int[][] computeIncidenceMatrix(int[][] pre, int[][] post) {
        int[][] c = new int[pre.length][pre[0].length];
        for (int i = 0; i < pre.length; i++) {
            // length returns number of rows
            //System.out.print("row " + i + " : ");
            for (int j = 0; j < pre[i].length; j++) {
                // here length returns # of columns corresponding to current row
                //System.out.print("col " + j + "  ");
                c[i][j] = post[i][j] - pre[i][j];
            }
        }
        return c;
    }

    public static int[][] computeIncidenceMatrix(Petrinet petrinet, BiMap<Place, Integer> placeToRowMap, BiMap<Transition, Integer> transitionToColumnMap) {
        return computeIncidenceMatrix(computePreIncidenceMatrix(petrinet, placeToRowMap, transitionToColumnMap), computePostIncidenceMatrix(petrinet, placeToRowMap, transitionToColumnMap));
    }


    public static int[][] computePreIncidenceMatrix(Petrinet petrinet, BiMap<Place, Integer> placeToRowMap, BiMap<Transition, Integer> transitionToColumnMap) {
        int[][] pre = new int[placeToRowMap.size()][transitionToColumnMap.size()];

        for (Place p : petrinet.getPlaces()) {
            Integer placeIndex = placeToRowMap.get(p);
            petrinet.getOutEdges(p).forEach(e -> {
                pre[placeIndex][transitionToColumnMap.get(e.getTarget())]++;
            });
        }
        return pre;
    }


    public static int[][] computePostIncidenceMatrix(Petrinet petrinet, BiMap<Place, Integer> placeToRowMap, BiMap<Transition, Integer> transitionToColumnMap) {
        int[][] post = new int[placeToRowMap.size()][transitionToColumnMap.size()];

        for (Transition t : petrinet.getTransitions()) {
            Integer transitionIndex = transitionToColumnMap.get(t);
            petrinet.getOutEdges(t).forEach(e -> {
                post[placeToRowMap.get(e.getTarget())][transitionIndex]++;
            });
        }
        return post;
    }

    public static int[][] transpose(int[][] pre) {
        int[][] trans_pre = new int[pre[0].length][pre.length];
        for (int k = 0; k < pre.length; k++) {
            for (int l = 0; l < pre[0].length; l++) {
                trans_pre[l][k] = pre[k][l];
            }
        }
        return trans_pre;
    }

    private void printMatrix(int[][] matrix, String label, BiMap<Place, Integer> placeToRowMap) {

        System.out.println("Printing " + label);

        String row = "";

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
