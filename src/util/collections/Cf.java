package util.collections;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Cf {

    /**
     * Returns a *modifiable* list containing the requested
     * values. The list is completely independent from the
     * array, t.i. their changes are NOT mutually reflected
     * (contrary to Arrays.asList()).
     */
    @NotNull
    public static <T> List<T> list(T... values) {
        List<T> res = newArrayList(values.length);
        Collections.addAll(res, values);
        return res;
    }

    @NotNull
    public static <T> List<T> list(T a) {
        List<T> ts = newList(1);
        ts.add(a);
        return ts;
    }

    @NotNull
    public static <T> List<T> list(T a, T b) {
        List<T> ts = newList(2);
        ts.add(a);
        ts.add(b);
        return ts;
    }

    @NotNull
    public static <T> List<T> list(T a, T b, T c) {
        List<T> ts = newList(3);
        ts.add(a);
        ts.add(b);
        ts.add(c);
        return ts;
    }


    @NotNull
    public static <T> ArrayList<T> newList(int initialCapacity) {
        return new ArrayList<T>(initialCapacity);
    }

    @NotNull
    public static <T> List<T> newList() {
        return newArrayList();
    }

    @NotNull
    public static <T> LinkedList<T> newLinkedList() {
        return new LinkedList<T>();
    }

    @NotNull
    public static <T> ArrayList<T> newArrayList() {
        return new ArrayList<T>();
    }

    @NotNull
    public static <T> ArrayList<T> newArrayList(int initialCapacity) {
        return new ArrayList<T>(initialCapacity);
    }


    @NotNull
    public static <K, V> ConcurrentMap<K, V> newConcurrentMap() {
        return new ConcurrentHashMap<K, V>();
    }

    @NotNull
    public static <K> Set<K> set(K... items) {
        return new LinkedHashSet<K>(Arrays.asList(items));
    }

    @NotNull
    public static <K> Set<K> set(Iterable<? extends K> iterable) {
        final Set<K> set = Cf.newUnorderedSet();
        for (final K k : iterable) {
            set.add(k);
        }
        return set;
    }

    @NotNull
    public static <T> Set<T> newUnorderedSet() {
        return new HashSet<T>();
    }

    @NotNull
    public static <K> List<K> list(Iterable<? extends K> items) {
        List<K> res = newArrayList();
        for (K item : items) {
            res.add(item);
        }
        return res;
    }

}
