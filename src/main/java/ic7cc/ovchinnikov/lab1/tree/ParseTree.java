package ic7cc.ovchinnikov.lab1.tree;

import java.util.*;

public class ParseTree {

    private String regex;
    private TreeNode<Map<String, String>> tree;
    private Deque<String> stack;
    private String output;
    private Map<Integer, Set<Integer>> followPos;

    public ParseTree(String regex) {
        this.regex = regex;
        this.output = "";
    }

    // TODO: Разобраться с говнокодом, слить ParseTree с TreeNode,
    //  перенести метрики firstpos, nullable, lastpos, followpos из Map в ноду дерева, убрать лишний код,
    //  понять нужен ли оператор +
    public TreeNode<Map<String, String>> build() {
        followPos = new TreeMap<>();

        String afterRegex = regex.replaceAll("\\s", "");
        afterRegex = buildReversePublishNotation(regex);

        Deque<TreeNode<Map<String, String>>> stack = new ArrayDeque<>();
        char[] chars = afterRegex.toCharArray();

        int numberL = 0;
        for (char ch : chars) {
            if (Character.isAlphabetic(ch) || ch == '#') {
                numberL++;

                Map<String, String> data = new HashMap<>();
                data.put("value", String.valueOf(ch));
                data.put("firstpos", String.valueOf(numberL));
                data.put("lastpos", String.valueOf(numberL));
                if (ch == 'e')
                    data.put("nullable", String.valueOf(true));
                else
                    data.put("nullable", String.valueOf(false));

                TreeNode<Map<String, String>> newNode = new TreeNode<>(data);
                stack.push(newNode);
            } else {
                Map<String, String> data = new HashMap<>();
                data.put("value", String.valueOf(ch));
                TreeNode<Map<String, String>> newNode = new TreeNode<>(data);
                List<TreeNode<Map<String, String>>> children = new LinkedList<>();

                int i = 0;
                List<String> nullable = new LinkedList<>();
                List<String> firstPos = new LinkedList<>();
                List<String> lastPos = new LinkedList<>();
                while (!stack.isEmpty()) {
                    TreeNode<Map<String, String>> popNode = stack.pop();
                    popNode.setParent(newNode);
                    children.add(0, popNode);

                    nullable.add(0,popNode.getData().get("nullable"));
                    firstPos.add(0,popNode.getData().get("firstpos"));
                    lastPos.add(0, popNode.getData().get("lastpos"));

                    i++;
                    if (isUnaryOperation(ch) && i == 1)
                        break;
                    else if (isBinaryOperation(ch) && i == 2)
                        break;
                }
                if (ch == '*') {
                    data.put("nullable", String.valueOf(true));

                    StringBuilder builder = new StringBuilder();
                    for (String s : firstPos) {
                        builder.append(s);
                        builder.append(" ");
                    }
                    data.put("firstpos", builder.toString());

                    builder = new StringBuilder();
                    for (String s : lastPos) {
                        builder.append(s);
                        builder.append(" ");
                    }
                    data.put("lastpos", builder.toString());

                    String[] splitLastPos = data.get("lastpos").split("\\s");
                    String[] splitFirstPos = data.get("firstpos").split("\\s");

                    Set<Integer> elems = new TreeSet<>();
                    for (String s : splitFirstPos) {
                        elems.add(Integer.parseInt(s));
                    }

                    for (String s : splitLastPos) {
                        Set<Integer> set = followPos.getOrDefault(Integer.parseInt(s), new TreeSet<>());
                        set.addAll(elems);
                        followPos.put(Integer.parseInt(s), set);
                    }

                } else if (ch == '+') {
                    data.put("nullable", String.valueOf(false));

                    StringBuilder builder = new StringBuilder();
                    for (String s : firstPos) {
                        builder.append(s);
                        builder.append(" ");
                    }
                    data.put("firstpos", builder.toString());

                    builder = new StringBuilder();
                    for (String s : lastPos) {
                        builder.append(s);
                        builder.append(" ");
                    }
                    data.put("lastpos", builder.toString());

                } else if (ch == '|') {
                    boolean n1 = Boolean.parseBoolean(nullable.get(0));
                    boolean n2 = Boolean.parseBoolean(nullable.get(1));

                    data.put("nullable", String.valueOf(n1 || n2));

                    StringBuilder builder = new StringBuilder();
                    for (String s : firstPos) {
                        builder.append(s);
                        builder.append(" ");
                    }
                    data.put("firstpos", builder.toString());

                    builder = new StringBuilder();
                    for (String s : lastPos) {
                        builder.append(s);
                        builder.append(" ");
                    }
                    data.put("lastpos", builder.toString());

                } else if (ch == '.') {
                    boolean n1 = Boolean.parseBoolean(nullable.get(0));
                    boolean n2 = Boolean.parseBoolean(nullable.get(1));

                    data.put("nullable", String.valueOf(n1 && n2));

                    if (n1) {
                        StringBuilder builder = new StringBuilder();
                        for (String s : firstPos) {
                            builder.append(s);
                            builder.append(" ");
                        }
                        data.put("firstpos", builder.toString());
                    } else {
                        data.put("firstpos", firstPos.get(0));
                    }

                    if (n2) {
                        StringBuilder builder = new StringBuilder();
                        for (String s : lastPos) {
                            builder.append(s);
                            builder.append(" ");
                        }
                        data.put("lastpos", builder.toString());
                    } else {
                        data.put("lastpos", lastPos.get(1));
                    }

                    String[] splitLastPos = children.get(0).getData().get("lastpos").split("\\s");
                    String[] splitFirstPos = children.get(1).getData().get("firstpos").split("\\s");

                    Set<Integer> elems = new TreeSet<>();
                    for (String s : splitFirstPos) {
                        elems.add(Integer.parseInt(s));
                    }

                    for (String s : splitLastPos) {
                        Set<Integer> set = followPos.getOrDefault(Integer.parseInt(s), new TreeSet<>());
                        set.addAll(elems);
                        followPos.put(Integer.parseInt(s), set);
                    }
                }

                newNode.setChildren(children);
                stack.push(newNode);
            }
        }

        if (stack.size() != 1)
            throw new RuntimeException();

        tree = stack.pop();
        return tree;
    }

    public Map<Integer, Set<Integer>> getFollowPos() {
        return followPos;
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

    private boolean isUnaryOperation(char oper) {
        return oper == '+' || oper == '*';
    }

    private boolean isBinaryOperation(char oper) {
        return oper == '|' || oper == '.';
    }
}
