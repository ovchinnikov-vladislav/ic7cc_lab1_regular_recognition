package ic7cc.ovchinnikov.lab1.util;

import java.util.ArrayDeque;
import java.util.Deque;

public class RPNRegex {

    private static Deque<String> stack;
    private static StringBuilder output;

    public static String build(String regex) {
        stack = new ArrayDeque<>();
        output = new StringBuilder();

        char[] chars = preProcessingRegex(regex);

        for (char ch : chars) {
            switch (ch) {
                case '|':
                    doIfReadOperation(ch + "", 1);
                    break;
                case '.':
                    doIfReadOperation(ch + "", 2);
                    break;
                case '*':
                case '+':
                    doIfReadOperation(ch + "", 3);
                    break;
                case '(':
                    stack.push(ch + "");
                    break;
                case ')':
                    doIfReadClosingBrackets();
                    break;
                case ' ':
                case '\t':
                case '\n':
                case '\r':
                    break;
                default:
                    output.append(ch);
                    break;
            }
        }
        while (!stack.isEmpty()) {
            output.append(stack.pop());
        }
        return output.toString();
    }

    private static char[] preProcessingRegex(String regex) {
        StringBuilder afterRegex = new StringBuilder();
        for (int i = 0; i < regex.length(); i++) {
            char ch = regex.charAt(i);
            afterRegex.append(ch);
            if (Character.isAlphabetic(ch) || Character.isDigit(ch) || ch == '*' || ch == '+' || ch == ')' || ch == '@') {
                if (i < regex.length() - 1) {
                    char nextChar = regex.charAt(i + 1);
                    if (Character.isAlphabetic(nextChar) || Character.isDigit(nextChar) || nextChar == '#' || nextChar == '(' || nextChar == '@')
                        afterRegex.append('.');
                }
            }
        }
        return afterRegex.toString().toCharArray();
    }

    private static void doIfReadOperation(String opThis, int priority) {
        while (!stack.isEmpty()) {
            String opTop = stack.pop();
            if (opTop.equals("(")) {
                stack.push(opTop);
                break;
            } else {
                int priorityNew;
                if (opTop.equals("|"))
                    priorityNew = 1;
                else if (opTop.equals("."))
                    priorityNew = 2;
                else
                    priorityNew = 3;
                if (priorityNew < priority) {
                    stack.push(opTop);
                    break;
                } else {
                    output.append(opTop);
                }
            }
        }
        stack.push(opThis);
    }

    private static void doIfReadClosingBrackets() {
        while (!stack.isEmpty()) {
            String chx = stack.pop();
            if (chx.equals("("))
                break;
            else
                output.append(chx);
        }
    }
}
