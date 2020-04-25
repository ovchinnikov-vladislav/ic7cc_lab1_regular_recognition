package ic7cc.ovchinnikov.lab1.fa;

import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.Node;
import ic7cc.ovchinnikov.lab1.exception.UmpossibleOperationException;
import ic7cc.ovchinnikov.lab1.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static guru.nidi.graphviz.attribute.Rank.RankDir.LEFT_TO_RIGHT;
import static guru.nidi.graphviz.model.Factory.*;

public class FA {

    private static final Logger logger = LoggerFactory.getLogger(FA.class);

    private Set<String> start;
    private Set<String> end;
    private Set<String> states;
    private Set<DTran<String>> trans;
    private Set<String> alphabet;

    public FA() {
        states = new TreeSet<>();
        trans = new HashSet<>();
        alphabet = new TreeSet<>();
        start = new HashSet<>();
        end = new HashSet<>();
    }

    public static FA buildDFA(String regex) {
        ParseTree tree = ParseTree.build(regex);
        return buildDFA(tree);
    }

    public static FA buildDFA(ParseTree tree) {
        if (tree == null)
            throw new UmpossibleOperationException("Invalid param: tree - " + null);

        FA dfa = new FA();
        Map<Boolean, List<Set<Integer>>> dStates = new HashMap<>();
        Set<DTran<Set<Integer>>> dTrans = new HashSet<>();

        dStates.put(true, new LinkedList<>());
        dStates.put(false, new LinkedList<>());

        var listFalse = dStates.get(false);

        Set<Integer> s = tree.getRoot().getFirstPos();
        listFalse.add(s);

        Set<Integer> dStart = s;

        Map<String, Set<Integer>> alphabetWithNumber = tree.getOperands();
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

        logger.info("Construction of a DFA by NFA: ");
        logger.info("\tStates of DFA: {}", dStates.get(true));
        logger.info("\tTrans of DFA: {}", dTrans);
        logger.info("\tAlphabet: {}", dfa.alphabet);
        logger.info("\tStart state: {}", dStart);
        logger.info("\tEnd state: {}", dEnd);

        FA resultDFA = renamingFA(dStates.get(true), dTrans, Collections.singleton(dStart), dEnd);
        resultDFA.alphabet = dfa.alphabet;

        return resultDFA;
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
        Set<DTran<Set<String>>> dTrans = new HashSet<>();

        dStates.put(true, new LinkedList<>());
        dStates.put(false, new LinkedList<>());

        Set<String> t = epsClosure(nfa.trans, nfa.start.toArray(String[]::new));

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

        logger.info("Construction of a DFA by NFA: ");
        logger.info("\tStates of DFA: {}", dStates.get(true));
        logger.info("\tTrans of DFA: {}", dTrans);
        logger.info("\tAlphabet: {}", dfa.alphabet);
        logger.info("\tStart state: {}", dStart);
        logger.info("\tEnd state: {}", dEnd);

        FA resultDFA = renamingFA(dStates.get(true), dTrans, Collections.singleton(dStart), dEnd);

        resultDFA.alphabet = dfa.alphabet;

        return resultDFA;
    }

    private static Set<String> epsClosure(Set<DTran<String>> dTrans, String... tStates) {
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

    public static FA minFA(FA fa) {
        return det(rec(det(rec(fa))));
    }

    private static <T> FA renamingFA(List<Set<T>> states, Set<DTran<Set<T>>> trans, Set<Set<T>> starts, Set<Set<T>> ends) {
        FA fa = new FA();

        int i = 1;
        Map<Set<T>, Integer> map = new HashMap<>();
        for (Set<T> dState : states) {
            fa.states.add(String.valueOf(i));
            map.put(dState, i);
            i++;
        }

        for (DTran<Set<T>> dTran : trans) {
            DTran<String> tran = new DTran<>();
            tran.startState = String.valueOf(map.get(dTran.startState));
            tran.endState = String.valueOf(map.get(dTran.endState));
            tran.value = dTran.value;
            fa.trans.add(tran);
        }

        for (Set<T> end : ends) {
            fa.end.add(String.valueOf(map.get(end)));
        }

        for (Set<T> start : starts) {
            fa.start.add(String.valueOf(map.get(start)));
        }

        return fa;
    }

    public boolean addTrans(String startState, String endState, String value) {
        DTran<String> dTran = new DTran<>();
        dTran.startState = startState;
        dTran.endState = endState;
        dTran.value = value;
        return trans.add(dTran);
    }

    public void printPNG(String fullName) throws IOException {
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

        Graphviz.fromGraph(g).width(900).render(Format.PNG).toFile(new File(fullName));
    }

    public boolean match(String value) {
        if (alphabet.size() == 0)
            throw new UnsupportedOperationException("FA not built");

        if (start.size() != 1)
            throw new UnsupportedOperationException("FA not built");

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
                throw new UnsupportedOperationException("FA nondeterministic");
            else if (nextStates.size() == 0)
                return false;

            nowState = nextStates.iterator().next();
        }

        return end.contains(nowState);
    }

    public Set<String> getStates() {
        return states;
    }

    public Set<DTran<String>> getTrans() {
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
}
