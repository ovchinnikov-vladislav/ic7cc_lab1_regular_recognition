package ic7cc.ovchinnikov.lab1.tree;

import java.util.*;

public class ParseTree {

    private String regex;
    private TreeNode<Pair<Integer, String>> tree;
    private Deque<String> stack;
    private String output;

    public ParseTree(String regex) {
        this.regex = regex;
        this.output = "";
    }

    public TreeNode<Pair<Integer, String>> build() {
        String afterRegex = regex.replaceAll("\\s", "");
        afterRegex = buildReversePublishNotation(regex);

        Queue<String> queue = new ArrayDeque<>();
        char[] chars = afterRegex.toCharArray();

        TreeNode<Pair<Integer, String>> root = new TreeNode<>(new Pair<>() {{firstValue = 0; secondValue = "";}});
        TreeNode<Pair<Integer, String>> selectNode = root;

        int numberL = 0;
        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            if (Character.isAlphabetic(ch) || ch == '#') {
                queue.add(ch + "");
            } else if (ch == '|' || ch == '*' || ch == '+' || ch == '.'){
                selectNode.setData(new Pair<>() {{firstValue = 0; secondValue = ch + "";}});
                while (!queue.isEmpty()) {
                    Pair<Integer, String> pair = new Pair<>();
                    pair.firstValue = ++numberL;
                    pair.secondValue = queue.remove() + "";
                    selectNode.addChild(pair);
                }
                if (i + 1 < chars.length) {
                    root = new TreeNode<>(new Pair<>() {{firstValue = 0; secondValue = "";}});
                    List<TreeNode<Pair<Integer, String>>> childrenNode = new LinkedList<>();
                    childrenNode.add(selectNode);
                    root.setChildren(childrenNode);
                    selectNode = root;
                }
            }
        }

        tree = root;
        return root;
    }

    private String buildReversePublishNotation(String regex) {
        stack = new ArrayDeque<>();

        char[] chars = preProcessingRegex(regex);

        for (char ch : chars) {
            switch (ch) {
                case '|':
                    gotOper(ch + "", 1);
                    break;
                case '.':
                    gotOper(ch + "", 2);
                    break;
                case '*':
                case '+':
                    gotOper(ch + "", 3);
                    break;
                case '(':
                    stack.push(ch + "");
                    break;
                case ')':
                    gotParen(ch + "");
                    break;
                default:
                    output += ch;
                    break;
            }
        }
        while (!stack.isEmpty()) {
            output += stack.pop();
        }
        return output;
    }

    private char[] preProcessingRegex(String regex) {
        String afterRegex = "";
        for (int i = 0; i < regex.length(); i++) {
            char ch = regex.charAt(i);
            afterRegex += ch;
            if (Character.isAlphabetic(ch) || ch == '*' || ch == '+') {
                if (i < regex.length() - 1) {
                    char nextChar = regex.charAt(i + 1);
                    if (Character.isAlphabetic(nextChar) || nextChar == '#' || nextChar == '(')
                        afterRegex += '.';
                }
            }
        }
        return afterRegex.toCharArray();
    }

    private void gotOper(String opThis, int prec1) {
        while (!stack.isEmpty()) {
            String opTop = stack.pop();
            if (opTop.equals("(")) {
                stack.push(opTop);
                break;
            } else {
                int prec2;
                if (opTop.equals("|"))
                    prec2 = 1;
                else
                    prec2 = 2;
                if (prec2 < prec1) {
                    stack.push(opTop);
                    break;
                } else {
                    output = output + opTop;
                }
            }
        }
        stack.push(opThis);
    }

    private void gotParen(String ch) {
        StringBuilder builder = new StringBuilder(output);
        while (!stack.isEmpty()) {
            String chx = stack.pop();
            if (chx.equals("("))
                break;
            else
                builder.append(chx);
        }
        output = builder.toString();
    }

    private static class Pair<T,S> {
        public T firstValue;
        public S secondValue;
    }
}
