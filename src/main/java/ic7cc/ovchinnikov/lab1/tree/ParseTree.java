package ic7cc.ovchinnikov.lab1.tree;

import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.Node;

import static guru.nidi.graphviz.model.Factory.*;

import ic7cc.ovchinnikov.lab1.exception.UmpossibleOperationException;
import ic7cc.ovchinnikov.lab1.util.RPNRegex;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ParseTree {

    private ParseTreeNode root;
    private Deque<ParseTreeNode> stack;

    private Map<Integer, Set<Integer>> followPos;
    private Map<String, Set<Integer>> operands;

    private ParseTree() {}

    public static ParseTree build(String regex) {
        if (regex == null)
            throw new UmpossibleOperationException("Regex: " + null);

        if (regex.isEmpty())
            regex = "@";

        ParseTree parseTree = new ParseTree();

        regex = "(" + regex + ")#";
        parseTree.followPos = new TreeMap<>();
        parseTree.operands = new TreeMap<>();
        parseTree.stack = new ArrayDeque<>();

        char[] rpnRegexString = RPNRegex.build(regex).toCharArray();

        int numberL = 0;
        for (char ch : rpnRegexString) {
            if (Character.isAlphabetic(ch) || Character.isDigit(ch) || ch == '@' || ch == '#') {
                numberL++;
                ParseTreeNode node = createOperandNode(parseTree, ch, numberL);
                if (ch != '#') {
                    Set<Integer> numbers = parseTree.operands.getOrDefault(String.valueOf(ch), new TreeSet<>());
                    numbers.add(numberL);
                    parseTree.operands.put(String.valueOf(ch), numbers);
                }
                parseTree.stack.push(node);
            } else if (isBinaryOperation(ch) || isUnaryOperation(ch)) {
                ParseTreeNode node = createOperationNode(parseTree, ch);
                parseTree.stack.push(node);
            }
        }

        if (parseTree.stack.size() != 1)
            throw new UmpossibleOperationException("The stack is empty, the root was not found, perhaps the regular expression is incorrectly composed.");

        parseTree.root = parseTree.stack.pop();
        return parseTree;
    }

    public ParseTreeNode getRoot() {
        return root;
    }

    private static ParseTreeNode createOperandNode(ParseTree tree, char operand, int numberL) {
        ParseTreeNode node = new ParseTreeNode(String.valueOf(operand));
        node.type = ParseTreeNode.TypeOperation.NOT_OPERATION;
        if (operand == '@') {
            node.type = ParseTreeNode.TypeOperation.EPSILON;
            node.nullable = true;
            node.firstPos = Set.of();
            node.lastPos = Set.of(numberL);
            return node;
        }

        node.firstPos.add(numberL);
        node.lastPos.add(numberL);

        if (operand == '#') {
            tree.followPos.put(numberL, null);
        }

        return node;
    }

    private static ParseTreeNode createOperationNode(ParseTree tree, char operation) {
        ParseTreeNode node = new ParseTreeNode(String.valueOf(operation));
        node.type = checkOperation(operation);
        node.binaryOperation = isBinaryOperation(operation);
        node.unaryOperation = isUnaryOperation(operation);

        int i = 0;
        List<? super TreeNode<String>> children = new LinkedList<>();
        while (!tree.stack.isEmpty()) {
            ParseTreeNode popNode = tree.stack.pop();
            popNode.setParent(node);
            children.add(0, popNode);
            i++;
            if (node.unaryOperation && i == 1 || node.binaryOperation && i == 2)
                break;
        }

        switch (node.type) {
            case STAR:
                doIfOperationStar(tree, node, children);
                break;
            case OR:
                doIfOperationOr(node, children);
                break;
            case CAT:
                doIfOperationCat(tree, node, children);
                break;
        }

        node.setChildren(children);

        return node;
    }

    private static void doIfOperationStar(ParseTree tree, ParseTreeNode node, List<? super TreeNode<String>> children) {
        node.nullable = true;

        for (Object object : children) {
            ParseTreeNode childNode = (ParseTreeNode) object;
            node.firstPos.addAll(childNode.firstPos);
            node.lastPos.addAll(childNode.lastPos);
        }

        for (Integer l : node.lastPos) {
            Set<Integer> set = tree.followPos.getOrDefault(l, new TreeSet<>());
            set.addAll(node.firstPos);
            tree.followPos.put(l, set);
        }

        node.setChildren(children);
    }

    private static void doIfOperationOr(ParseTreeNode node, List<? super TreeNode<String>> children) {
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

    private static void doIfOperationCat(ParseTree tree, ParseTreeNode node, List<? super TreeNode<String>> children) {
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
            Set<Integer> set = tree.followPos.getOrDefault(l, new TreeSet<>());
            set.addAll(((ParseTreeNode) children.get(1)).firstPos);
            tree.followPos.put(l, set);
        }

        node.setChildren(children);
    }

    public Map<Integer, Set<Integer>> getFollowPos() {
        return Collections.unmodifiableMap(followPos);
    }

    public Map<String, Set<Integer>> getOperands() {
        return Collections.unmodifiableMap(operands);
    }

    private static boolean isUnaryOperation(char oper) {
        return oper == '+' || oper == '*';
    }

    private static boolean isBinaryOperation(char oper) {
        return oper == '|' || oper == '.';
    }

    private static ParseTreeNode.TypeOperation checkOperation(char operation) {
        switch (operation) {
            case '*':
                return ParseTreeNode.TypeOperation.STAR;
            case '|':
                return ParseTreeNode.TypeOperation.OR;
            case '.':
                return ParseTreeNode.TypeOperation.CAT;
            default:
                throw new UmpossibleOperationException("Operation undefined: " + operation);
        }
    }

    public void printPNG(String fullName) throws IOException {
        Deque<ParseTreeNode> deque = new ArrayDeque<>();
        deque.push(root);

        Graph g = graph("Parse Tree").directed();
        while (!deque.isEmpty()) {
            ParseTreeNode popNode = deque.pop();
            Node oldNode = node(popNode.getId().toString()).with(Label.html("\"" + popNode.getData() +
                    "\" (fp: " + popNode.getFirstPos() +
                    " lp: " + popNode.getLastPos() + ")"));
            for (Object obj : popNode.getChildren()) {
                ParseTreeNode n = (ParseTreeNode) obj;
                Node newNode = node(n.getId().toString()).with(Label.html("\"" + n.getData() +
                        "\" (fp: " + n.getFirstPos() +
                        " lp: " + n.getLastPos() + ")"));
                oldNode = oldNode.link(to(newNode));
                deque.push(n);
            }
            g = g.with(oldNode);
        }

        Graphviz.fromGraph(g).width(900).render(Format.PNG).toFile(new File(fullName));
    }

    public static class ParseTreeNode extends TreeNode<String> {

        private final UUID id;
        private boolean unaryOperation;
        private boolean binaryOperation;
        private boolean nullable;
        private Set<Integer> firstPos;
        private Set<Integer> lastPos;
        private TypeOperation type;

        public ParseTreeNode(String data) {
            super(data);
            this.id = UUID.randomUUID();
            this.firstPos = new TreeSet<>();
            this.lastPos = new TreeSet<>();
            this.unaryOperation = false;
            this.binaryOperation = false;
            this.nullable = false;
        }

        public UUID getId() {
            return id;
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
}
