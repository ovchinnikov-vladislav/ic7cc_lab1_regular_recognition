package ic7cc.ovchinnikov.lab1.fa;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.Node;
import ic7cc.ovchinnikov.lab1.tree.ParseTree;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static guru.nidi.graphviz.attribute.Rank.RankDir.LEFT_TO_RIGHT;
import static guru.nidi.graphviz.model.Factory.*;

public class FA {

    private Set<String> start;
    private Set<String> end;
    private Set<String> states;
    private List<DTran<String>> trans;
    private Set<String> alphabet;

    public FA() {
        states = new TreeSet<>();
        trans = new ArrayList<>();
        alphabet = new TreeSet<>();
        start = new HashSet<>();
        end = new HashSet<>();
    }

    public static FA buildDFA(ParseTree tree) {
        FA dfa = new FA();

        Map<Boolean, List<Set<Integer>>> dStates = new HashMap<>();
        dStates.put(true, new LinkedList<>());
        dStates.put(false, new LinkedList<>());

        List<DTran<Set<Integer>>> dTrans = new ArrayList<>();
        Set<Integer> s = tree.getRoot().getFirstPos();

        var listFalse = dStates.get(false);
        listFalse.add(s);

        Set<Integer> dStart = s;

        Map<String, Set<Integer>> alphabetWithNumber = tree.getNumberAndCharMap();
        while (dStates.get(false).size() > 0) {

            s = dStates.get(false).remove(0);
            dStates.get(true).add(s);

            for (Map.Entry<String, Set<Integer>> entry : alphabetWithNumber.entrySet()) {
                String ch = entry.getKey();
                dfa.alphabet.add(ch);
                Set<Integer> numbers = entry.getValue();
                Set<Integer> u = new TreeSet<>();
                for (Integer p : s) {
                    if (numbers.contains(p)) {
                        u.addAll(tree.getFollowPos().get(p));
                    }
                }

                if (!u.isEmpty()) {
                    if (!dStates.get(true).contains(u) && !dStates.get(false).contains(u)) {
                        dStates.get(false).add(u);
                    }

                    DTran<Set<Integer>> dTran = new DTran<>();
                    dTran.startState = s;
                    dTran.value = ch;
                    dTran.endState = u;
                    dTrans.add(dTran);
                }

            }
        }
        Set<Set<Integer>> dEnd = new HashSet<>();
        Integer numberEnd = null;
        for (Map.Entry<Integer, Set<Integer>> entry : tree.getFollowPos().entrySet()) {
            if (entry.getValue() == null)
                numberEnd = entry.getKey();
        }

        for (Set<Integer> state : dStates.get(true)) {
            if (state.contains(numberEnd))
                dEnd.add(state);
        }

        System.out.println("Log buildDFA:");
        System.out.println("\tDFA: " + dStates.get(true));
        System.out.println("\tDFA: " + dTrans);
        System.out.println("\tAlphabet: " + dfa.alphabet);
        System.out.println("\tStart: " + dStart);
        System.out.println("\tEnd: " + dEnd + "\n");

        int i = 1;
        Map<Set<Integer>, Integer> map = new HashMap<>();
        for (Set<Integer> dState : dStates.get(true)) {
            dfa.states.add(String.valueOf(i));
            map.put(dState, i);
            i++;
        }

        for (DTran<Set<Integer>> dTran : dTrans) {
            DTran<String> tran = new DTran<>();
            tran.startState = String.valueOf(map.get(dTran.startState));
            tran.endState = String.valueOf(map.get(dTran.endState));
            tran.value = dTran.value;
            dfa.trans.add(tran);
        }

        for (Set<Integer> end : dEnd) {
            dfa.end.add(String.valueOf(map.get(end)));
        }

        dfa.start = new HashSet<>(Collections.singleton(String.valueOf(map.get(dStart))));

        return dfa;
    }

    public Set<String> getStates() {
        return states;
    }

    public List<DTran<String>> getTrans() {
        return trans;
    }

    public Set<String> getStart() {
        return start;
    }

    public Set<String> getEnd() {
        return end;
    }

    public Set<String> getAlphabet() {
        return alphabet;
    }

    public static FA rec(FA fa) {
        FA rFA = new FA();
        rFA.start = fa.end;
        rFA.end = fa.start;
        rFA.states = new HashSet<>(fa.states);
        rFA.alphabet = new TreeSet<>(fa.alphabet);

        for (DTran<String> dfaDTran : fa.trans) {
            DTran<String> rDfaDTran = new DTran<>();
            rDfaDTran.startState = dfaDTran.endState;
            rDfaDTran.value = dfaDTran.value;
            rDfaDTran.endState = dfaDTran.startState;
            rFA.trans.add(rDfaDTran);
        }

        return rFA;
    }

    public static FA det(FA nfa) {
        FA dfa = new FA();

        Map<Boolean, List<Set<String>>> dStates = new HashMap<>();
        dStates.put(true, new LinkedList<>());
        dStates.put(false, new LinkedList<>());

        Set<String> t = epsClosure(nfa.trans, nfa.start.toArray(String[]::new));
        List<DTran<Set<String>>> dTrans = new ArrayList<>();

        var listFalse = dStates.get(false);
        listFalse.add(t);

        Set<String> dStart = t;
        while (dStates.get(false).size() > 0) {

            t = dStates.get(false).remove(0);
            dStates.get(true).add(t);

            for (String a : nfa.alphabet) {
                if (a.equals("@"))
                    continue;
                Set<String> move = new HashSet<>();
                for (String st : t) {
                    for (DTran<String> dTran : nfa.trans) {
                        if (dTran.startState.equals(st) && dTran.value.equals(a))
                            move.add(dTran.endState);
                    }
                }
                Set<String> u = epsClosure(nfa.trans, move.toArray(String[]::new));

                if (!u.isEmpty()) {
                    if (!dStates.get(true).contains(u) && !dStates.get(false).contains(u)) {
                        dStates.get(false).add(u);
                    }

                    DTran<Set<String>> dTran = new DTran<>();
                    dTran.startState = t;
                    dTran.value = a;
                    dTran.endState = u;
                    dTrans.add(dTran);
                }
            }
        }

        Set<Set<String>> dEnd = new HashSet<>();
        for (String nEnd : nfa.end) {
            for (Set<String> dState : dStates.get(true)) {
                if (dState.contains(nEnd))
                    dEnd.add(dState);
            }
        }
        dfa.alphabet = new TreeSet<>(nfa.alphabet);

        System.out.println("Log det:");
        System.out.println("\tDFA: " + dStates.get(true));
        System.out.println("\tDFA: " + dTrans);
        System.out.println("\tAlphabet: " + dfa.alphabet);
        System.out.println("\tStart: " + dStart);
        System.out.println("\tEnd: " + dEnd + "\n");

        int i = 1;
        Map<Set<String>, Integer> map = new HashMap<>();
        for (Set<String> dState : dStates.get(true)) {
            dfa.states.add(String.valueOf(i));
            map.put(dState, i);
            i++;
        }

        for (DTran<Set<String>> dTran : dTrans) {
            DTran<String> tran = new DTran<>();
            tran.startState = String.valueOf(map.get(dTran.startState));
            tran.endState = String.valueOf(map.get(dTran.endState));
            tran.value = dTran.value;
            dfa.trans.add(tran);
        }

        for (Set<String> end : dEnd) {
            dfa.end.add(String.valueOf(map.get(end)));
        }

        dfa.start = new HashSet<>();
        dfa.start.add(String.valueOf(map.get(dStart)));

        return dfa;
    }

    private static Set<String> epsClosure(List<DTran<String>> dTrans, String... tStates) {
        Set<String> epsClosureStates = new HashSet<>();

        Deque<String> deque = new ArrayDeque<>();

        for (String eps : tStates)
            deque.push(eps);

        while (!deque.isEmpty()) {
            String state = deque.pop();
            epsClosureStates.add(state);
            for (DTran<String> dTran : dTrans) {
                if (dTran.startState.equals(state) && dTran.value.equals("@")) {
                    deque.push(dTran.endState);
                }
            }
        }

        return epsClosureStates;
    }

    private static class DTran<T> {
        private T startState;
        private String value;
        private T endState;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DTran<?> dTran = (DTran<?>) o;
            return Objects.equals(startState, dTran.startState) &&
                    Objects.equals(value, dTran.value) &&
                    Objects.equals(endState, dTran.endState);
        }

        @Override
        public int hashCode() {
            return Objects.hash(startState, value, endState);
        }

        @Override
        public String toString() {
            return "(" + startState + " -> " + value + " -> " + endState + ")";
        }
    }

    public static FA minFA(FA fa) {
        return det(rec(det(rec(fa))));
    }

    public boolean addTrans(String startState, String endState, String value) {
        DTran<String> dTran = new DTran<>();
        dTran.startState = startState;
        dTran.endState = endState;
        dTran.value = value;
        return trans.add(dTran);
    }

    public void printFA() throws IOException {
        Graph g = graph("FA").directed().graphAttr().with(Rank.dir(LEFT_TO_RIGHT));
        for (DTran<String> tran : trans) {
            Node startNode = node(tran.startState);
            if (end.contains(tran.startState))
                startNode = startNode.with(Style.FILLED);
            Node endNode = node(tran.endState);
            if (end.contains(tran.endState))
                endNode = endNode.with(Style.FILLED);
            startNode = startNode.link(to(endNode).with(Label.html(tran.value)));
            g = g.with(startNode);
        }

        Graphviz.fromGraph(g).width(900).render(Format.PNG).toFile(new File("graph/fa_"+ UUID.randomUUID() +".png"));
    }

    public boolean match(String value) {
        if (start.size() != 1)
            throw new UnsupportedOperationException("Конечный автомат недетерминирован");

        String nowState = start.iterator().next();

        char[] chars = value.toCharArray();
        for (char c : chars) {
            Set<String> nextStates = new HashSet<>();
            for (DTran<String> dTran : trans) {
                if (dTran.startState.equals(nowState) && dTran.value.equals(String.valueOf(c))) {
                    nextStates.add(dTran.endState);
                }
            }

            if (nextStates.size() > 1)
                throw new UnsupportedOperationException("Конечный автомат недетерминирован");
            else if (nextStates.size() == 0)
                return false;

            nowState = nextStates.iterator().next();
        }

        return end.contains(nowState);
    }
}
