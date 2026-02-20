package de.happybavarian07.coolstufflib.service.util;

public final class Tuples {
    private Tuples() {}

    public static <A, B> Tuple2<A, B> of(A a, B b) {
        return new Tuple2<>(a, b);
    }

    public record Tuple2<A, B>(A first, B second) {
    }
}

