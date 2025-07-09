package de.happybavarian07.coolstufflib.jpa.interfaces;

import java.lang.reflect.Field;

public interface ResultSetValueConverter {
    Object convert(Field field, Object value);
}
