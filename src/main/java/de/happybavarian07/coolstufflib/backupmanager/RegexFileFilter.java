package de.happybavarian07.coolstufflib.backupmanager;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

public class RegexFileFilter implements FileFilter {
    private final Pattern pattern;

    public RegexFileFilter(String regex) {
        this.pattern = Pattern.compile(regex);
    }

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