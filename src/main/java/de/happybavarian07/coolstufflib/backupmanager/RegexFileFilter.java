package de.happybavarian07.coolstufflib.backupmanager;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

public class RegexFileFilter implements FileFilter {
    private final Pattern pattern;

    /**
     * <p>Creates a filter with the given regex.</p>
     *
     * @param regex The regex pattern
     */
    public RegexFileFilter(String regex) {
        this.pattern = Pattern.compile(regex);
    }

    /**
     * <p>Tests whether the file name matches the regex pattern.</p>
     *
     * @param file The file to test
     * @return {@code true} if it matches
     */
    @Override
    public boolean accept(File file) {
        return pattern.matcher(file.getName()).matches();
    }

    @Override
    public String toString() {
        return "RegexFileFilter{" + "pattern=" + pattern +
                '}';
    }
}