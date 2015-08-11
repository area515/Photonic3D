package org.area515.resinprinter.inkdetection.visual;

import java.util.Comparator;
import java.util.TreeSet;

public class FixedSizePriorityQueue<E> extends TreeSet<E> {
	private static final long serialVersionUID = -8732145546954107942L;
	private final int maxSize;

    public FixedSizePriorityQueue(int maxSize) {
        super((Comparator<E>)null);
        this.maxSize = maxSize;
    }

    public FixedSizePriorityQueue(int maxSize, Comparator<E> comparator) {
        super(comparator);
        this.maxSize = maxSize;
    }

    /**
     * @return true if element was added, false otherwise
     * */
    @Override
    public boolean add(E e) {
        if (maxSize == 0) {
            // max size was initiated to zero => just return false
            return false;
        } else if (maxSize - size() > 0) {
            // queue isn't full => add element and decrement elementsLeft
            return super.add(e);
        } else {
            // there is already 1 or more elements => compare to the least
            int compared = super.comparator().compare(e, this.first());
            if (compared > 0) {
                // new element is larger than the least in queue => pull the least and add new one to queue
                pollFirst();
                super.remove(e);
                super.add(e);
                return true;
            } else {
                // new element is less than the least in queue => return false
                return false;
            }
        }
    }
}