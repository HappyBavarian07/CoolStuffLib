package de.happybavarian07.coolstufflib.configstuff.advanced.section.internal;

import de.happybavarian07.coolstufflib.configstuff.advanced.section.BaseConfigSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.ListSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.MapSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.SetSection;

/**
 * Enum specifying the type/kind of ConfigSection to create.
 * Determines the concrete class used when creating sections via unified API.
 */
public enum SectionKind {
    /** Generic section - uses BaseConfigSection for flexible key-value storage */
    DEFAULT,
    
    /** Map-optimized section - uses MapSection for key-value operations */
    MAP,
    
    /** List-optimized section - uses ListSection for ordered collections */
    LIST,
    
    /** Set-optimized section - uses SetSection for unique values */
    SET;

    /**
     * Maps a ConfigSection subclass to its corresponding SectionKind.
     * Returns null if the class doesn't match any known kind.
     *
     * @param clazz The Class object to check
     * @return Matching SectionKind or null
     */
    public static SectionKind fromClass(Class<?> clazz) {
        if (clazz == null) return null;
        
        String className = clazz.getSimpleName();
        return switch (className) {
            case "MapSection" -> MAP;
            case "ListSection" -> LIST;
            case "SetSection" -> SET;
            default -> {
                if (clazz.getName().contains("BaseConfigSection")) {
                    yield DEFAULT;
                }
                yield null;
            }
        };
    }

    /**
     * Gets the concrete ConfigSection class for this kind.
     *
     * @return The corresponding section implementation class
     */
    public Class<?> getSectionClass() {
        return switch (this) {
            case DEFAULT -> BaseConfigSection.class;
            case MAP -> MapSection.class;
            case LIST -> ListSection.class;
            case SET -> SetSection.class;
            default -> throw new IllegalStateException("Unknown SectionKind: " + this);
        };
    }
}