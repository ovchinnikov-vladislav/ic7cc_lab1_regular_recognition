/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package ic7cc.ovchinnikov.lab1;

import ic7cc.ovchinnikov.lab1.fa.FA;
import ic7cc.ovchinnikov.lab1.tree.ParseTree;
import ic7cc.ovchinnikov.lab1.util.RPNRegex;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class FATest {

    private static final Logger logger = LoggerFactory.getLogger(FATest.class);

    @Test
    public void testRPN() {
        String rpn = RPNRegex.build("a|b*(a|b)*");

        assertEquals("ab*ab|*.|", rpn);
    }

    @Test
    public void testFirstPosRoot() throws Exception {
        ParseTree parseTree = ParseTree.build("(a|b)*abb@");

        parseTree.printPNG("test/test_first_pos_root/parse_tree.png");

        ParseTree.ParseTreeNode treeRoot = parseTree.getRoot();

        assertEquals(Set.of(1, 2, 3), treeRoot.getFirstPos());
    }

    @Test
    public void testLastPosRoot() throws Exception {
        ParseTree parseTree = ParseTree.build("(a|b)*abb");

        parseTree.printPNG("test/test_last_pos_root/parse_tree.png");

        ParseTree.ParseTreeNode treeRoot = parseTree.getRoot();

        assertEquals(Set.of(6), treeRoot.getLastPos());
    }

    @Test
    public void testBuildParseTreeAndFollowPos() throws Exception {
        ParseTree parseTree = ParseTree.build("(a|b)*abb");

        ParseTree.ParseTreeNode treeRoot = parseTree.getRoot();

        parseTree.printPNG("test/test_build_parse_tree_and_follow_pos/parse_tree.png");

        Map<Integer, Set<Integer>> expFollowPos = new HashMap<>();
        expFollowPos.put(1, Set.of(1, 2, 3));
        expFollowPos.put(2, Set.of(1, 2, 3));
        expFollowPos.put(3, Set.of(4));
        expFollowPos.put(4, Set.of(5));
        expFollowPos.put(5, Set.of(6));
        expFollowPos.put(6, null);

        Map<Integer, Set<Integer>> followPos = parseTree.getFollowPos();

        assertEquals(expFollowPos, followPos);
    }

    @Test
    public void testDetFAAndMinFA() throws Exception {
        FA fa = new FA();
        fa.getAlphabet().add("a");
        fa.getAlphabet().add("b");
        fa.getAlphabet().add("@");

        fa.getStates().add("0");
        fa.getStates().add("1");
        fa.getStates().add("2");
        fa.getStates().add("3");
        fa.getStates().add("4");
        fa.getStates().add("5");
        fa.getStates().add("6");
        fa.getStates().add("7");
        fa.getStates().add("8");
        fa.getStates().add("9");
        fa.getStates().add("10");

        fa.getStart().add("0");
        fa.getEnd().add("10");

        fa.addTrans("0", "1", "@");
        fa.addTrans("0", "7", "@");
        fa.addTrans("1","2", "@");
        fa.addTrans("1", "4", "@");
        fa.addTrans("2", "3", "a");
        fa.addTrans("3", "6", "@");
        fa.addTrans("4", "5", "b");
        fa.addTrans("5", "6", "@");
        fa.addTrans("6", "1", "@");
        fa.addTrans("6", "7", "@");
        fa.addTrans("7", "8", "a");
        fa.addTrans("8", "9", "b");
        fa.addTrans("9", "10", "b");

        logger.info("До минимизации: ");
        logger.info("\tСостояния ДКА: {}", fa.getStates());
        logger.info("\tПути ДКА: {}", fa.getTrans());
        logger.info("\tАлфавит: {}", fa.getAlphabet());
        logger.info("\tНачальные состояния: {}", fa.getStart());
        logger.info("\tКонечные состояния: {}\n", fa.getEnd());

        fa.printPNG("test/test_det_min_fa/nfa.png");

        FA detFA = FA.det(fa);

        detFA.printPNG("test/test_det_min_fa/dfa.png");

        assertTrue(detFA.match("ababb"));
        assertFalse(detFA.match("ab"));

        FA minFA = FA.minFA(fa);

        System.out.println("После минимизации: ");
        System.out.println("\tСостояния ДКА: " + minFA.getStates());
        System.out.println("\tПути ДКА: " + minFA.getTrans());
        System.out.println("\tАлфавит: " + minFA.getAlphabet());
        System.out.println("\tНачальное состояние: " + minFA.getStart());
        System.out.println("\tКонечные состояния: " + minFA.getEnd() + "\n");

        minFA.printPNG("test/test_det_min_fa/min_fa.png");

        assertTrue(minFA.match("ababb"));
        assertFalse(minFA.match("ab"));

        assertTrue(minFA.getAlphabet().contains("a"));
        assertTrue(minFA.getAlphabet().contains("b"));
        assertEquals(3, minFA.getAlphabet().size());
        assertEquals(1, minFA.getStart().size());
        assertEquals(1, minFA.getEnd().size());
        assertNotEquals(fa.getStates().size(), minFA.getStates().size());
        assertNotEquals(fa.getTrans().size(), minFA.getTrans().size());
    }

    @Test
    public void testBuildDFAFromRegex() throws IOException {

        String regex = "(a(b|a)d*bdae*)|(ab|de)*";

        FA fa = FA.buildDFA(regex);

        fa.printPNG("test/test_build_dfa_from_regex/dfa.png");

        assertTrue(fa.match("abdbdae"));

    }

    @Test
    public void testBuildDFAFromParseTree() throws IOException {
        String regex = "(a(b|a)d*bdae*)|(ab|de)*";

        ParseTree tree = ParseTree.build("(ab|ad*bdae*)|(ab|de)*");

        tree.printPNG("test/test_build_dfa_from_parse_tree/parse_tree.png");

        FA fa = FA.buildDFA(regex);

        fa.printPNG("test/test_build_dfa_from_parse_tree/dfa.png");

        FA minfa = FA.minFA(fa);

        minfa.printPNG("test/test_build_dfa_from_parse_tree/minfa.png");

        assertTrue(fa.match("abdbdae"));
    }

    @Test
    public void testMatches() throws IOException {

        String regex = "((a|b)*abb)*";

        FA fa = FA.buildDFA(regex);

        fa.printPNG("test/test_matches/dfa.png");

        assertTrue(fa.match(""));

        String testInput = "abb";
        assertTrue(fa.match("abb"));
        for (int i = 0; i < 10; i++) {
            testInput = "a" + testInput;
            assertTrue(fa.match(testInput));
            testInput = "b" + testInput;
            assertTrue(fa.match(testInput));
        }

        assertFalse(fa.match("a"));
        assertFalse(fa.match("b"));
    }
}
