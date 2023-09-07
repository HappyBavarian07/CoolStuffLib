package de.happybavarian07.coolstufflib.commandmanagement;/*
 * @Author HappyBavarian07
 * @Date 06.11.2021 | 12:22
 */

import java.util.*;

/**
 * PaginatedList class.
 */
public class PaginatedList<T> {
    private final Map<Integer, List<T>> resultMap;
    private final List<T> listOfThings;
    private int maxItemsPerPage = 1;
    private boolean sorted = false;

    /**
     * Constructs a new PaginatedList with the given list of things.
     *
     * @param listOfThings the list of things to be stored in the PaginatedList
     */
    public PaginatedList(List<T> listOfThings) {
        this.listOfThings = listOfThings;
        this.resultMap = new HashMap<>();
    }

    /**
     * Constructs a new PaginatedList with the given set of elements.
     *
     * @param listOfThings the set of elements to be added to the PaginatedList
     */
    public PaginatedList(Set<T> listOfThings) {
        this.listOfThings = new ArrayList<>();
        this.listOfThings.addAll(listOfThings);
        resultMap = new HashMap<>();
    }

    /**
     * Retrieves the result map of the PaginatedList.
     *
     * @return A Map of Integer and List of T objects representing the result map of the PaginatedList
     * @throws ListNotSortedException if the List has not been sorted yet
     */
    public Map<Integer, List<T>> getResultMap() throws ListNotSortedException {
        if (!sorted) throw new ListNotSortedException("The List isn't sorted yet! (Method: getResultMap())");
        return resultMap;
    }

    /**
     * Gets the maximum number of items per page for the PaginatedList.
     *
     * @return the maximum number of items per page
     */
    public int getMaxItemsPerPage() {
        return maxItemsPerPage;
    }

    /**
     * Sets the maximum number of items per page.
     *
     * @param maxItemsPerPage The maximum number of items per page.
     * @return The PaginatedList instance.
     */
    public PaginatedList<T> maxItemsPerPage(int maxItemsPerPage) {
        this.maxItemsPerPage = maxItemsPerPage;
        return this;
    }

    /**
     * Retrieves the list of things stored in the PaginatedList.
     *
     * @return a List of type T containing the list of things stored in the PaginatedList
     */
    public List<T> getListOfThings() {
        return listOfThings;
    }

    /**
     * Sorts the list of things in the PaginatedList.
     * <p>
     * This method clears the resultMap, creates a temporary list of things, and adds each item to the list. If the count is equal to the maxItemsPerPage or the last item in the list is reached, the list is added to the resultMap and the count and currentPage are reset. If the resultMap is not empty, the sorted boolean is set to true.
     *
     * @return the sorted PaginatedList
     */
    public PaginatedList<T> sort() {
        if (sorted) return this;
        resultMap.clear();
        int count = 0;
        int currentPage = 1;
        List<T> tempObs = new ArrayList<>();
        for (T sub : listOfThings) {
            tempObs.add(sub);
            if (count == (maxItemsPerPage - 1) || listOfThings.get(listOfThings.size() - 1) == sub) {
                List<T> tempTempObs = new ArrayList<>(tempObs);
                resultMap.put(currentPage, tempTempObs);
                count = 0;
                currentPage += 1;
                tempObs.clear();
            } else {
                count++;
            }
        }
        if (!resultMap.isEmpty()) {
            sorted = true;
        }
        return this;
    }

    /**
     * Retrieves the page of the list.
     *
     * @param page the page number to retrieve
     * @return the list of elements in the page
     * @throws ListNotSortedException if the list is not sorted
     */
    public List<T> getPage(int page) throws ListNotSortedException {
        if (!sorted) throw new ListNotSortedException("The List isn't sorted yet! (Method: getPage())");
        return getResultMap().get(page);
    }

    /**
     * Checks if the PaginatedList contains the given page.
     *
     * @param page the page to check for
     * @return true if the PaginatedList contains the given page, false otherwise
     * @throws ListNotSortedException if the list is not sorted
     */
    public boolean containsPage(int page) throws ListNotSortedException {
        if (!sorted) throw new ListNotSortedException("The List isn't sorted yet! (Method: containsPage())");
        return getResultMap().containsKey(page);
    }

    /**
     * Retrieves the maximum page number of the list.
     *
     * @return the maximum page number of the list
     * @throws ListNotSortedException if the list is not sorted
     */
    public int getMaxPage() throws ListNotSortedException {
        if (!sorted) throw new ListNotSortedException("The List isn't sorted yet! (Method: getMaxPage())");
        return getResultMap().size();
    }

    /**
     * Thrown when a list is not sorted.
     */
    public static class ListNotSortedException extends Exception {

        /**
         * Constructs a new ListNotSortedException.
         */
        public ListNotSortedException() {
        }

        /**
         * Throws an exception when a list is not sorted.
         *
         * @param message The message to be displayed when the exception is thrown.
         */
        public ListNotSortedException(String message) {
            super(message);
        }

        /**
         * Constructs a new ListNotSortedException with the specified detail message and cause.
         *
         * @param message the detail message
         * @param cause   the cause
         */
        public ListNotSortedException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Constructs a new ListNotSortedException with the specified cause.
         *
         * @param cause the cause of the exception
         */
        public ListNotSortedException(Throwable cause) {
            super(cause);
        }

        /**
         * Constructs a new ListNotSortedException with the specified detail message, cause,
         * suppression enabled or disabled, and writable stack trace enabled or disabled.
         *
         * @param message            The detail message.
         * @param cause              The cause. (A null value is permitted, indicating that
         *                           the cause is nonexistent or unknown.)
         * @param enableSuppression  Whether suppression is enabled or disabled.
         * @param writableStackTrace Whether the stack trace should be writable.
         * @throws IllegalArgumentException if the message is null.
         */
        public ListNotSortedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }
}
