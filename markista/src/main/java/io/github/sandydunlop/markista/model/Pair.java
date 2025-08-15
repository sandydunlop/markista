package io.github.sandydunlop.markista.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class Pair<L extends Serializable,R extends Serializable> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private L l;
    private R r;

    public Pair(L l, R r) {
        this.l = l;
        this.r = r;
    }
    
    public L getL() {
        return l;
    }

    public R getR() {
        return r;
    }

    public void setL(L l) {
        this.l = l;
    }

    public void setR(R r) {
        this.r = r;
    }

    public String toString() {
        return "Pair[" + l + "," + r + "]";
    }

    public boolean equals(Object other) {
        return other instanceof Pair<?,?> pair &&
            Objects.equals(l, pair.l) &&
            Objects.equals(r, pair.r);
    }

    public int hashCode() {
        if (l == null) return (r == null) ? 0 : r.hashCode() + 1;
        else if (r == null) return l.hashCode() + 2;
        else return l.hashCode() * 17 + r.hashCode();
    }

    public static <A extends Serializable,B extends Serializable> Pair<A,B> of(A a, B b) {
        return new Pair<>(a,b);
    }
}
