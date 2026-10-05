package de.happybavarian07.coolstufflib.commandmanagement;

import java.util.Objects;

public record ArgumentOption<T>(String value, T resolved, String permission) {
    public ArgumentOption {
        Objects.requireNonNull(value);
    }

    public ArgumentOption(String value, T resolved) {
        this(value, resolved, null);
    }
}
