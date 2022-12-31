package backend;

import java.io.Serializable;
import java.util.Comparator;

public class CustomComparator <T extends Object & Comparable<T>> implements Comparator<T>, Serializable {

	@Override
	public int compare(T o1, T o2) {
		return o1.compareTo(o2);
	}

}
