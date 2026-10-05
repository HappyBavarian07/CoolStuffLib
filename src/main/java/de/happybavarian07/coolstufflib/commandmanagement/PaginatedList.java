package de.happybavarian07.coolstufflib.commandmanagement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PaginatedList<T extends Comparable<T>> {
    private final List<T> items;
    private final Map<Integer, List<T>> resultMap = new LinkedHashMap<>();
    private int maxItemsPerPage = 1;
    private boolean sorted;

    public PaginatedList(List<T> items) {
        this.items = new ArrayList<>(items);
    }

    public PaginatedList(Set<T> items) {
        this(new ArrayList<>(items));
    }

    public Map<Integer, List<T>> getResultMap() throws ListNotSortedException {
        ensurePrepared();
        return Map.copyOf(resultMap);
    }

    public int getMaxItemsPerPage() {
        return maxItemsPerPage;
    }

    public PaginatedList<T> maxItemsPerPage(int maxItemsPerPage) {
        if (maxItemsPerPage < 1) throw new IllegalArgumentException("maxItemsPerPage must be at least 1");
        this.maxItemsPerPage = maxItemsPerPage;
        sorted = false;
        return this;
    }

    public List<T> getListOfThings() {
        return List.copyOf(items);
    }

    public PaginatedList<T> sort(String sortingAlgorithm, boolean reSort) {
        if (sorted && !reSort) return this;
        Comparator<T> comparator = comparatorFor(sortingAlgorithm);
        if (comparator != null) items.sort(comparator);
        sorted = true;
        rebuildPages();
        return this;
    }

    public PaginatedList<T> prepare(Comparator<? super T> comparator) {
        items.sort(comparator);
        sorted = true;
        rebuildPages();
        return this;
    }

    public List<T> getPage(int page) throws ListNotSortedException {
        ensurePrepared();
        return resultMap.getOrDefault(page, List.of());
    }

    public List<T> page(int page) {
        if (page < 1) return List.of();
        if (!sorted) rebuildPages();
        return resultMap.getOrDefault(page, List.of());
    }

    public boolean containsPage(int page) throws ListNotSortedException {
        ensurePrepared();
        return resultMap.containsKey(page);
    }

    public boolean hasPage(int page) {
        return !page(page).isEmpty();
    }

    public int getMaxPage() throws ListNotSortedException {
        ensurePrepared();
        return resultMap.size();
    }

    public int pageCount() {
        if (!sorted) rebuildPages();
        return Math.max(1, resultMap.size());
    }

    @SuppressWarnings("unchecked")
    private Comparator<T> comparatorFor(String sortingAlgorithm) {
        return switch (sortingAlgorithm.toLowerCase()) {
            case "alphabetic" -> Comparator.comparing(Object::toString, String.CASE_INSENSITIVE_ORDER);
            case "subcommand" -> (first, second) -> ((SubCommand) first).path().compareTo(((SubCommand) second).path());
            case "alphanumeric" -> Comparator.comparing(Object::toString, new NaturalOrderComparator());
            case "none", "default" -> null;
            default -> throw new IllegalArgumentException("Unknown sorting algorithm: " + sortingAlgorithm);
        };
    }

    private void rebuildPages() {
        resultMap.clear();
        for (int from = 0, page = 1; from < items.size(); from += maxItemsPerPage, page++) {
            int to = Math.min(from + maxItemsPerPage, items.size());
            resultMap.put(page, List.copyOf(items.subList(from, to)));
        }
    }

    private void ensurePrepared() throws ListNotSortedException {
        if (!sorted) throw new ListNotSortedException("The list is not prepared yet");
    }

    public static class SubCommandComparator implements Comparator<SubCommand> {
        @Override
        public int compare(SubCommand first, SubCommand second) {
            return first.path().compareTo(second.path());
        }
    }

    public static class ListNotSortedException extends Exception {
        public ListNotSortedException() {
        }

        public ListNotSortedException(String message) {
            super(message);
        }
    }
}
