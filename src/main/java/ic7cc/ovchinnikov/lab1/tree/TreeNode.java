package ic7cc.ovchinnikov.lab1.tree;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class TreeNode<T> implements Iterable<TreeNode<T>> {

    private T data;
    private TreeNode<T> parent;
    private List<? super TreeNode<T>> children;
    private List<? super TreeNode<T>> elementIndex;

    public TreeNode(T data) {
        this.data = data;
        this.children = new LinkedList<>();
        this.elementIndex = new LinkedList<>();
        this.elementIndex.add(this);
    }

    public TreeNode<T> addChild(T child) {
        TreeNode<T> childNode = new TreeNode<T>(child);
        childNode.parent = this;
        this.children.add(childNode);
        this.registerChildForSearch(childNode);
        return childNode;
    }

    public int getLevel() {
        if (this.isRoot())
            return 0;
        else
            return parent.getLevel() + 1;
    }

    public Object findTreeNode(Comparable<T> cmp) {
        for (Object element : this.elementIndex) {
            T elData = ((TreeNode<T>) element).data;
            if (cmp.compareTo(elData) == 0)
                return element;
        }
        return null;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public boolean isLeaf() {
        return children.size() == 0;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public TreeNode<T> getParent() {
        return parent;
    }

    public void setParent(TreeNode<T> parent) {
        this.parent = parent;
    }

    public List<? super TreeNode<T>> getChildren() {
        return children;
    }

    public void setChildren(List<? super TreeNode<T>> children) {
        this.children = children;
    }

    private void registerChildForSearch(TreeNode<T> node) {
        elementIndex.add(node);
        if (parent != null)
            parent.registerChildForSearch(node);
    }

    @Override
    public String toString() {
        return data != null ? data.toString() : "[data null]";
    }

    @Override
    public Iterator<TreeNode<T>> iterator() {
        TreeNodeIterator<T> iterator = new TreeNodeIterator<>(this);
        return iterator;
    }
}
