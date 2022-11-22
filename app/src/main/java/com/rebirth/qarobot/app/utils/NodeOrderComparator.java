package com.rebirth.qarobot.app.utils;

import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Comparator;

public class NodeOrderComparator implements Comparator<Node> {
    @Override
    public int compare(Node o1, Node o2) {
        return Integer.compare(getOrder(o1), getOrder(o2));
    }

    private int getOrder(Node node) {
        NamedNodeMap attributes = node.getAttributes();
        Attr orderAttr = (Attr) attributes.getNamedItem("order");
        if (orderAttr != null) {
            return Integer.parseInt(orderAttr.getValue());
        } else return 0;
    }
}
