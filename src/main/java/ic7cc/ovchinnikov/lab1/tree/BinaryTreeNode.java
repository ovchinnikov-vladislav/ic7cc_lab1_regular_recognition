package ic7cc.ovchinnikov.lab1.tree;

import java.util.LinkedList;
import java.util.List;

public class BinaryTreeNode<T> {

    private T data;
    private BinaryTreeNode<T> parent;
    private BinaryTreeNode<T> left;
    private BinaryTreeNode<T> right;
    private List<BinaryTreeNode<T>> elementIndex;

    public BinaryTreeNode(T data) {
        this.data = data;
        this.elementIndex = new LinkedList<>();
        this.elementIndex.add(this);
    }

    public BinaryTreeNode<T> addLeftChild(T child) {
        BinaryTreeNode<T> childNode = new BinaryTreeNode<T>(child);
        childNode.parent = this;
        this.left = childNode;
        this.registerChildForSearch(childNode);
        return childNode;
    }

    public BinaryTreeNode<T> addRightChild(T child) {
        BinaryTreeNode<T> childNode = new BinaryTreeNode<T>(child);
        childNode.parent = this;
        this.right = childNode;
        this.registerChildForSearch(childNode);
        return childNode;
    }

    public int getLevel() {
        if (this.isRoot())
            return 0;
        else
            return parent.getLevel() + 1;
    }

    public BinaryTreeNode<T> findTreeNode(Comparable<T> cmp) {
        for (BinaryTreeNode<T> element : this.elementIndex) {
            T elData = element.data;
            if (cmp.compareTo(elData) == 0)
                return element;
        }
        return null;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public boolean isLeaf() {
        return left == null && right == null;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public BinaryTreeNode<T> getParent() {
        return parent;
    }

    public void setParent(BinaryTreeNode<T> parent) {
        this.parent = parent;
    }

    public List<BinaryTreeNode<T>> getChildren() {
        return new LinkedList<>() {
            {
                add(left);
                add(right);
            }
        };
    }

    public void setChildren(BinaryTreeNode<T> left, BinaryTreeNode<T> right) {
        this.left = left;
        this.right = right;
    }

    private void registerChildForSearch(BinaryTreeNode<T> node) {
        elementIndex.add(node);
        if (parent != null)
            parent.registerChildForSearch(node);
    }

    @Override
    public String toString() {
        return data != null ? data.toString() : "[data null]";
    }

}
