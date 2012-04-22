package util.collections;

import java.util.Iterator;

@SuppressWarnings("unchecked")
public class Cu {


    public static <T> Iterable<T> iterable(final Iterator<T> iter) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return (Iterator<T>) iter; //safe
            }
        };
    }

}
