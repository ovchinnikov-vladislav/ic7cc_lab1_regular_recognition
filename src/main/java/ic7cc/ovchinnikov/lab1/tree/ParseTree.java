package ic7cc.ovchinnikov.lab1.tree;

import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.Node;

import static guru.nidi.graphviz.attribute.Label.Justification.LEFT;
import static guru.nidi.graphviz.model.Factory.*;
import static guru.nidi.graphviz.attribute.Records.*;
import static guru.nidi.graphviz.model.Compass.*;

import ic7cc.ovchinnikov.lab1.exception.UncaughtOperationException;
import ic7cc.ovchinnikov.lab1.util.RPNRegex;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

public class ParseTree {

    private ParseTreeNode root;
    private Deque<ParseTreeNode> stack;

    private Map<Integer, Set<Integer>> followPos;
    private Map<String, Set<Integer>> numberAndCharMap;

    public ParseTreeNode build(String regex) {
        if (regex == null)
            throw new UncaughtOperationException("Регулярное выражение не может быть: " + null);

        regex = "(" + regex + ")#";
        followPos = new TreeMap<>();
        numberAndCharMap = new TreeMap<>();
        stack = new ArrayDeque<>();

        char[] rpnRegexString = RPNRegex.build(regex).toCharArray();

        int numberL = 0;
        for (char ch : rpnRegexString) {
            if (Character.isAlphabetic(ch) || Character.isDigit(ch) || ch == '@' || ch == '#') {
                numberL++;
                ParseTreeNode node = createOperandNode(ch, numberL);
                if (ch != '#') {
                    Set<Integer> numbers = numberAndCharMap.getOrDefault(String.valueOf(ch), new TreeSet<>());
                    numbers.add(numberL);
                    numberAndCharMap.put(String.valueOf(ch), numbers);
                }
                stack.push(node);
            } else if (isBinaryOperation(ch) || isUnaryOperation(ch)) {
                ParseTreeNode node = createOperationNode(ch);
                stack.push(node);
            }
        }

        if (stack.size() != 1)
            throw new UncaughtOperationException("Стек пуст, корень не найден, возможно регулярное выражение некорректно составлено.");

        root = stack.pop();
        return root;
    }

    public ParseTreeNode getRoot() {
        return root;
    }

    private ParseTreeNode createOperandNode(char ch, int numberL) {
        ParseTreeNode node = new ParseTreeNode(String.valueOf(ch));
        node.type = ParseTreeNode.TypeOperation.NOT_OPERATION;
        if (ch == '@') {
            node.type = ParseTreeNode.TypeOperation.EPSILON;
            node.nullable = true;
            node.firstPos = Set.of();
            node.lastPos = Set.of(numberL);
            return node;
        }

        node.firstPos.add(numberL);
        node.lastPos.add(numberL);

        if (ch == '#') {
            followPos.put(numberL, null);
        }

        return node;
    }

    private ParseTreeNode createOperationNode(char ch) {
        ParseTreeNode node = new ParseTreeNode(String.valueOf(ch));
        node.type = checkOperation(ch);
        node.binaryOperation = isBinaryOperation(ch);
        node.unaryOperation = isUnaryOperation(ch);

        int i = 0;
        List<? super TreeNode<String>> children = new LinkedList<>();
        while (!stack.isEmpty()) {
            ParseTreeNode popNode = stack.pop();
            popNode.setParent(node);
            children.add(0, popNode);
            i++;
            if (node.unaryOperation && i == 1 || node.binaryOperation && i == 2)
                break;
        }

        switch (node.type) {
            case STAR:
                doIfOperationStar(node, children);
                break;
            case OR:
                doIfOperationOr(node, children);
                break;
            case CAT:
                doIfOperationCat(node, children);
                break;
        }

        node.setChildren(children);

        return node;
    }

    private void doIfOperationStar(ParseTreeNode node, List<? super TreeNode<String>> children) {
        node.nullable = true;

        for (Object object : children) {
            ParseTreeNode childNode = (ParseTreeNode) object;
            node.firstPos.addAll(childNode.firstPos);
            node.lastPos.addAll(childNode.lastPos);
        }

        for (Integer l : node.lastPos) {
            Set<Integer> set = followPos.getOrDefault(l, new TreeSet<>());
            set.addAll(node.firstPos);
            followPos.put(l, set);
        }

        node.setChildren(children);
    }

    private void doIfOperationOr(ParseTreeNode node, List<? super TreeNode<String>> children) {
        List<Boolean> nullable = new ArrayList<>();

        for (Object object : children) {
            ParseTreeNode childNode = (ParseTreeNode) object;
            if (childNode.type != ParseTreeNode.TypeOperation.EPSILON) {
                node.firstPos.addAll(childNode.firstPos);
            }
            node.lastPos.addAll(childNode.lastPos);
            nullable.add(childNode.nullable);
        }

        boolean n = false;
        for (boolean b : nullable) {
            if (b) {
                n = true;
                break;
            }
        }
        node.nullable = n;

        node.setChildren(children);
    }

    private void doIfOperationCat(ParseTreeNode node, List<? super TreeNode<String>> children) {
        List<Boolean> nullable = new ArrayList<>();
        List<Set<Integer>> firstPosChildren = new ArrayList<>();
        List<Set<Integer>> lastPosChildren = new ArrayList<>();

        for (Object object : children) {
            ParseTreeNode childNode = (ParseTreeNode) object;
            nullable.add(childNode.nullable);
            if (childNode.type != ParseTreeNode.TypeOperation.EPSILON) {
                firstPosChildren.add(childNode.firstPos);
            }
            lastPosChildren.add(childNode.lastPos);
        }

        node.nullable = nullable.get(0) && nullable.get(1);

        if (nullable.get(0)) {
            for (Set<Integer> firstPos : firstPosChildren)
                node.firstPos.addAll(firstPos);
        } else {
            node.firstPos = firstPosChildren.get(0);
        }

        if (nullable.get(1)) {
            for (Set<Integer> lastPos : lastPosChildren)
                node.lastPos.addAll(lastPos);
        } else {
            node.lastPos = lastPosChildren.get(1);
        }

        for (Integer l : ((ParseTreeNode) children.get(0)).lastPos) {
            Set<Integer> set = followPos.getOrDefault(l, new TreeSet<>());
            set.addAll(((ParseTreeNode) children.get(1)).firstPos);
            followPos.put(l, set);
        }

        node.setChildren(children);
    }

    public Map<Integer, Set<Integer>> getFollowPos() {
        return Collections.unmodifiableMap(followPos);
    }

    public Map<String, Set<Integer>> getNumberAndCharMap() {
        return Collections.unmodifiableMap(numberAndCharMap);
    }

    private boolean isUnaryOperation(char oper) {
        return oper == '+' || oper == '*';
    }

    private boolean isBinaryOperation(char oper) {
        return oper == '|' || oper == '.';
    }

    private ParseTreeNode.TypeOperation checkOperation(char oper) {
        switch (oper) {
            case '*':
                return ParseTreeNode.TypeOperation.STAR;
            case '|':
                return ParseTreeNode.TypeOperation.OR;
            case '.':
                return ParseTreeNode.TypeOperation.CAT;
            default:
                throw new UncaughtOperationException("Неопределена операция: " + oper);
        }
    }

    public static class ParseTreeNode extends TreeNode<String> {

        private boolean unaryOperation;
        private boolean binaryOperation;
        private boolean nullable;
        private Set<Integer> firstPos;
        private Set<Integer> lastPos;
        private TypeOperation type;

        public ParseTreeNode(String data) {
            super(data);
            this.firstPos = new TreeSet<>();
            this.lastPos = new TreeSet<>();
            this.unaryOperation = false;
            this.binaryOperation = false;
            this.nullable = false;
        }

        public boolean isUnaryOperation() {
            return unaryOperation;
        }

        public boolean isBinaryOperation() {
            return binaryOperation;
        }

        public boolean isNullable() {
            return nullable;
        }

        public Set<Integer> getFirstPos() {
            return firstPos;
        }

        public Set<Integer> getLastPos() {
            return lastPos;
        }

        public TypeOperation getType() {
            return type;
        }

        private enum TypeOperation {
            STAR, OR, CAT, NOT_OPERATION, EPSILON
        }
    }

    public void printTree() throws IOException {
        class PairNode {
            private Node first;
            private ParseTreeNode second;

            PairNode(Node first, ParseTreeNode second) { this.first = first; this.second = second; }
        }

        Deque<PairNode> deque = new ArrayDeque<>();
        int i = 0;
        Node rootNode = node(String.valueOf(i)).with(Label.html("\"" + root.getData() +
                "\" (fp: " + root.getFirstPos() +
                " lp: " + root.getLastPos() + ")"));
        PairNode pair = new PairNode(rootNode, root);
        deque.push(pair);

        Graph g = graph("Parse Tree").directed();
        while (!deque.isEmpty()) {
            PairNode popNode = deque.pop();
            Node oldNode = popNode.first;
            for (Object obj : popNode.second.getChildren()) {
                if (obj instanceof ParseTreeNode) {
                    ParseTreeNode n = (ParseTreeNode) obj;
                    Node newNode = node(String.valueOf(++i)).with(Label.html("\"" + n.getData() +
                            "\" (fp: " + n.getFirstPos() +
                            " lp: " + n.getLastPos() + ")"));
                    oldNode = oldNode.link(to(newNode));
                    deque.push(new PairNode(newNode, n));
                }
            }
            g = g.with(oldNode);
        }

        Graphviz.fromGraph(g).width(900).render(Format.PNG).toFile(new File("graph/parse_tree_"+ UUID.randomUUID() +".png"));
    }
}
