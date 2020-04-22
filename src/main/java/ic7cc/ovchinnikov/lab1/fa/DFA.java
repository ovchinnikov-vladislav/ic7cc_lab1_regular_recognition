package ic7cc.ovchinnikov.lab1.fa;

import ic7cc.ovchinnikov.lab1.tree.ParseTree;

import java.util.*;

public class DFA {

    private Set<Integer> start;
    private Set<Integer> end;
    private Map<Set<Integer>, Boolean> dStates;
    private Map<DTranPair, Set<Integer>> dTrans;

    public DFA() {
        dStates = new HashMap<>();
        dTrans = new HashMap<>();
    }

    public void build(ParseTree tree) {
        Set<Integer> s = tree.getRoot().getFirstPos();
        dStates.put(s, false);
        start = s;

        while (dStates.containsValue(false)) {
            s = getUnmarkedDStates();
            dStates.put(s, true);

            Map<String, Set<Integer>> map = tree.getNumberAndCharMap();
            for (Map.Entry<String, Set<Integer>> entry : map.entrySet()) {
                String ch = entry.getKey();
                Set<Integer> numbers = entry.getValue();
                Set<Integer> u = new TreeSet<>();
                for (Integer p : s) {
                    if (numbers.contains(p))
                        u.addAll(tree.getFollowPos().get(p));
                }

                if (!dStates.containsKey(u))
                    dStates.put(u, false);

                DTranPair pair = new DTranPair();
                pair.states = s;
                pair.value = ch;
                dTrans.put(pair, u);
            }
        }
        end = s;
    }

    public Map<Set<Integer>, Boolean> getDStates() {
        return dStates;
    }

    public Map<DTranPair, Set<Integer>> getDTrans() {
        return dTrans;
    }

    private Set<Integer> getUnmarkedDStates() {
        for (Map.Entry<Set<Integer>, Boolean> entry : dStates.entrySet()) {
            if (!entry.getValue())
                return entry.getKey();
        }
        return null;
    }

    public Set<Integer> getStart() {
        return start;
    }

    public Set<Integer> getEnd() {
        return end;
    }

    private static class DTranPair {
        private Set<Integer> states;
        private String value;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DTranPair dTranPair = (DTranPair) o;
            return Objects.equals(states, dTranPair.states) &&
                    Objects.equals(value, dTranPair.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(states, value);
        }

        @Override
        public String toString() {
            return "(" + states + ", " + value + ")";
        }
    }
}
