package ic7cc.ovchinnikov.lab1.tree;

import java.util.Iterator;

public class TreeNodeIterator<T> implements Iterator<TreeNode<T>> {

    private TreeNode<T> node;
    private ProcessStages doNext;
    private Object next;
    private Iterator<? super TreeNode<T>> childrenCurNodeIterator;
    private Iterator<? super TreeNode<T>> childrenSubNodeIterator;

    public TreeNodeIterator(TreeNode<T> node) {
        this.node = node;
        this.doNext = ProcessStages.PROCESS_PARENT;
        this.childrenCurNodeIterator = node.getChildren().iterator();
    }

    @Override
    public boolean hasNext() {
        if (this.doNext == ProcessStages.PROCESS_PARENT) {
            this.next = this.node;
            this.doNext = ProcessStages.PROCESS_CHILD_CUR_NODE;
            return true;
        }

        if (this.doNext == ProcessStages.PROCESS_CHILD_CUR_NODE) {
            if (childrenCurNodeIterator.hasNext()) {
                Object childDirect = childrenCurNodeIterator.next();
                childrenSubNodeIterator = ((TreeNode<T>) childDirect).iterator();
                this.doNext = ProcessStages.PROCESS_CHILD_SUB_NODE;
                return hasNext();
            } else {
                this.doNext = null;
                return false;
            }
        }

        if (this.doNext == ProcessStages.PROCESS_CHILD_SUB_NODE) {
            if (childrenSubNodeIterator.hasNext()) {
                this.next = childrenSubNodeIterator.next();
                return true;
            } else {
                this.next = null;
                this.doNext = ProcessStages.PROCESS_CHILD_CUR_NODE;
                return hasNext();
            }
        }

        return false;
    }

    @Override
    public TreeNode<T> next() {
        return (TreeNode<T>) this.next;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    enum ProcessStages {
        PROCESS_PARENT, PROCESS_CHILD_CUR_NODE, PROCESS_CHILD_SUB_NODE
    }
}
