package com.dillard.games.checkers;

import java.util.List;
import java.util.Random;

/**
 * Uses a Sum Tree
 */
public class PrioritizedSampler {
    private Random random;
    private Node root = null;

    public PrioritizedSampler(Random random) {
        this.random = random;
    }

    public synchronized TrainingExample sampleAndRemove() {
        if (root == null) {
            throw new RuntimeException("The tree is empty");
        }

        double threshold = random.nextDouble() * root.prioritySum;

        Node node = root;
        while (node.left != null && node.right != null) {
            if (node.left.prioritySum > threshold) {
                node = node.left;
            } else {
                threshold -= node.left.prioritySum;
                node = node.right;
            }
        }

        // node.left and node.right are null. we found a leaf.
        TrainingExample te = node.te; // want to return this

        if (node.parent == null) {
            // leaf has no parent means that it is the root
            root = null;
            return te;
        }

        Node parent = node.parent;
        Node remainingChild = parent.left == node ? parent.right : parent.left;
        parent.replaceWithChild(remainingChild);
        propagateSums(parent.parent);
        return te;
    }

    public synchronized void addAll(List<TrainingExample> examples) {
        for (var te : examples) {
            add(te);
        }
    }

    public synchronized void add(TrainingExample example) {
        if (root == null) {
            root = new Node(example, example.priority, null);
            return;
        }

        Node node = root;
        while (node.left != null && node.right != null) {
            if (node.left.subtreeSize < node.right.subtreeSize) {
                node = node.left;
            } else {
                node = node.right;
            }
        }

        // Move this node's TE to the right
        node.right = new Node(node.te, node.te.priority, node);
        node.te = null;

        // add a child to the left
        node.left = new Node(example, example.priority, node);

        // propagate sums up the tree
        propagateSums(node);
    }

    private void propagateSums(Node node) {
        if (node != null) {
            node.subtreeSize = node.left.subtreeSize + node.right.subtreeSize;
            node.prioritySum = node.left.prioritySum + node.right.prioritySum;
            propagateSums(node.parent);
        }
    }

    public double getPrioritySum() {
        if (root == null) {
            return 0;
        }
        return root.prioritySum;
    }

    public int getNodeCount() {
        if (root == null) {
            return 0;
        }
        return root.subtreeSize;
    }

    private class Node {
        TrainingExample te;
        Node left;
        Node right;
        Node parent;
        int subtreeSize;
        double prioritySum;

        public Node(TrainingExample te, double prioritySum, Node parent) {
            this.te = te;
            this.prioritySum = prioritySum;
            this.subtreeSize = 1;
            this.parent = parent;
        }

        public void replaceWithChild(Node child) {
            this.te = child.te;
            this.prioritySum = child.prioritySum;
            this.left = child.left;
            if (child.left != null) {
                child.left.parent = this;
            }
            this.right = child.right;
            if (child.right != null) {
                child.right.parent = this;
            }
            this.subtreeSize = child.subtreeSize;
        }

        @Override
        public String toString() {
            return (parent == null ? "x" : "p") + " " +
                    (left == null ? "x" : "l") + " " +
                    (right == null ? "x" : "r") + " " +
                    (te == null ? "x" : "T") + " " +
                    subtreeSize + " " + prioritySum;
        }
    }
}
