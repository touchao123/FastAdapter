package com.mikepenz.fastadapter_extensions.utilities;

import android.util.Log;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.IExpandable;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.IItemAdapter;
import com.mikepenz.fastadapter.ISubItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * Created by flisar on 15.09.2016.
 */
public class SubItemUtil {

    /**
     * returns a set of selected items, regardless of their visibility
     *
     * @param adapter the adapter instance
     * @return a set of all selected items and subitems
     */
    public static Set<IItem> getSelectedItems(FastAdapter adapter) {
        Set<IItem> selections = new HashSet<>();
        int length = adapter.getItemCount();
        List<IItem> items = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            items.add(adapter.getItem(i));
        }
        updateSelectedItemsWithCollapsed(selections, items);
        return selections;
    }

    private static void updateSelectedItemsWithCollapsed(Set<IItem> selected, List<IItem> items) {
        int length = items.size();
        for (int i = 0; i < length; i++) {
            if (items.get(i).isSelected()) {
                selected.add(items.get(i));
            }
            if (items.get(i) instanceof IExpandable && ((IExpandable) items.get(i)).getSubItems() != null) {
                updateSelectedItemsWithCollapsed(selected, ((IExpandable) items.get(i)).getSubItems());
            }
        }
    }

    /**
     * counts the items in the adapter, respecting subitems regardless of there current visibility
     *
     * @param adapter   the adapter instance
     * @param predicate predicate against which each item will be checked before counting it
     * @return number of items in the adapter that apply to the predicate
     */
    public static int countItems(final IItemAdapter adapter, IPredicate predicate) {
        return countItems(adapter.getAdapterItems(), true, false, predicate);
    }

    /**
     * counts the items in the adapter, respecting subitems regardless of there current visibility
     *
     * @param adapter      the adapter instance
     * @param countHeaders if true, headers will be counted as well
     * @return number of items in the adapter
     */
    public static int countItems(final IItemAdapter adapter, boolean countHeaders) {
        return countItems(adapter.getAdapterItems(), countHeaders, false, null);
    }

    private static int countItems(List<IItem> items, boolean countHeaders, boolean subItemsOnly, IPredicate predicate) {
        return getAllItems(items, countHeaders, subItemsOnly, predicate).size();
    }

    /**
     * retrieves a list of the items in the adapter, respecting subitems regardless of there current visibility
     *
     * @param adapter   the adapter instance
     * @param predicate predicate against which each item will be checked before adding it to the result
     * @return list of items in the adapter that apply to the predicate
     */
    public static List<IItem> getAllItems(final IItemAdapter adapter, IPredicate predicate) {
        return getAllItems(adapter.getAdapterItems(), true, false, predicate);
    }

    /**
     * retrieves a list of the items in the adapter, respecting subitems regardless of there current visibility
     *
     * @param adapter      the adapter instance
     * @param countHeaders if true, headers will be counted as well
     * @return list of items in the adapter
     */
    public static List<IItem> getAllItems(final IItemAdapter adapter, boolean countHeaders) {
        return getAllItems(adapter.getAdapterItems(), countHeaders, false, null);
    }

    private static List<IItem> getAllItems(List<IItem> items, boolean countHeaders, boolean subItemsOnly, IPredicate predicate) {
        List<IItem> res = new ArrayList<>();
        if (items == null || items.size() == 0) {
            return res;
        }

        int temp;
        int itemCount = items.size();
        IItem item;
        List<IItem> subItems;
        for (int i = 0; i < itemCount; i++) {
            item = items.get(i);
            if (item instanceof IExpandable && ((IExpandable) item).getSubItems() != null) {
                subItems = ((IExpandable) item).getSubItems();
                if (predicate == null) {
                    if (subItems != null && subItems.size() > 0)
                        res.addAll(subItems);
                    res.addAll(getAllItems(subItems, countHeaders, true, predicate));
                    if (countHeaders) {
                        res.add(item);
                    }
                } else {
                    temp = subItems != null ? subItems.size() : 0;
                    for (int j = 0; j < temp; j++) {
                        if (predicate.apply(subItems.get(j))) {
                            res.add(subItems.get(j));
                        }
                    }
                    if (countHeaders && predicate.apply(item)) {
                        res.add(item);
                    }
                }
            }
            // in some cases, we must manually check, if the item is a sub item, process is optimised as much as possible via the subItemsOnly parameter already
            // sub items will be counted in above if statement!
            else if (!subItemsOnly && getParent(item) == null) {
                if (predicate == null) {
                    res.add(item);
                } else if (predicate.apply(item)) {
                    res.add(item);
                }
            }

        }
        return res;
    }

    /**
     * counts the selected items in the adapter underneath an expandable item, recursively
     *
     * @param adapter the adapter instance
     * @param header  the header who's selected children should be counted
     * @return number of selected items underneath the header
     */
    public static <T extends IItem & IExpandable> int countSelectedSubItems(final FastAdapter adapter, T header) {
        Set<IItem> selections = getSelectedItems(adapter);
        return countSelectedSubItems(selections, header);
    }

    public static <T extends IItem & IExpandable> int countSelectedSubItems(Set<IItem> selections, T header) {
        int count = 0;
        List<IItem> subItems = header.getSubItems();
        int items = header.getSubItems() != null ? header.getSubItems().size() : 0;
        for (int i = 0; i < items; i++) {
            if (selections.contains(subItems.get(i))) {
                count++;
            }
            if (subItems.get(i) instanceof IExpandable && ((IExpandable) subItems.get(i)).getSubItems() != null) {
                count += countSelectedSubItems(selections, (T) subItems.get(i));
            }
        }
        return count;
    }

    /**
     * select or unselect all sub itmes underneath an expandable item
     *
     * @param adapter the adapter instance
     * @param header  the header who's children should be selected or deselected
     * @param select the new selected state of the sub items
     */
    public static <T extends IItem & IExpandable> void selectAllSubItems(final FastAdapter adapter, T header, boolean select) {
        selectAllSubItems(adapter, header, select, false);
    }

    /**
     * select or unselect all sub itmes underneath an expandable item
     *
     * @param adapter the adapter instance
     * @param header  the header who's children should be selected or deselected
     * @param select the new selected state of the sub items
     * @param notifyParent true, if the parent should be notified about the changes of it's children selection state
     */
    public static <T extends IItem & IExpandable> void selectAllSubItems(final FastAdapter adapter, T header, boolean select, boolean notifyParent) {
        int subItems = header.getSubItems().size();
        int position = adapter.getPosition(header);
        if (header.isExpanded()) {
            for (int i = 0; i < subItems; i++) {
                if (((IItem)header.getSubItems().get(i)).isSelectable()) {
                    if (select) {
                        adapter.select(position + i + 1);
                    } else {
                        adapter.deselect(position + i + 1);
                    }
                }
                if (header.getSubItems().get(i) instanceof IExpandable)
                    selectAllSubItems(adapter, header, select, notifyParent);

            }
        } else {
            for (int i = 0; i < subItems; i++) {
                if (((IItem)header.getSubItems().get(i)).isSelectable()) {
                    ((IItem) header.getSubItems().get(i)).withSetSelected(select);
                }
                if (header.getSubItems().get(i) instanceof IExpandable)
                    selectAllSubItems(adapter, header, select, notifyParent);
            }

        }

        // we must notify the view only!
        if (notifyParent && position >= 0) {
            adapter.notifyItemChanged(position);
        }
    }

    private static <T extends IExpandable & IItem> T getParent(IItem item) {

        if (item instanceof ISubItem) {
            return (T) ((ISubItem) item).getParent();
        }
        return null;
    }

    /**
     * deletes all selected items from the adapter respecting if the are sub items or not
     * subitems are removed from their parents sublists, main items are directly removed
     *
     * @param deleteEmptyHeaders if true, empty headers will be removed from the adapter
     * @return List of items that have been removed from the adapter
     */
    public static List<IItem> deleteSelected(final FastAdapter fastAdapter, boolean notifyParent, boolean deleteEmptyHeaders) {
        List<IItem> deleted = new ArrayList<>();

        // we use a LinkedList, because this has performance advantages when modifying the listIterator during iteration!
        // Modifying list is O(1)
        LinkedList<IItem> selectedItems = new LinkedList<>(getSelectedItems(fastAdapter));

        Log.d("DELETE", "selectedItems: " + selectedItems.size());

        // we delete item per item from the adapter directly or from the parent
        // if keepEmptyHeaders is false, we add empty headers to the selected items set via the iterator, so that they are processed in the loop as well
        IItem item, parent;
        int pos, parentPos;
        boolean expanded;
        ListIterator<IItem> it = selectedItems.listIterator();
        while (it.hasNext()) {
            item = it.next();

            pos = fastAdapter.getPosition(item);

            // search for parent - if we find one, we remove the item from the parent's subitems directly
            parent = getParent(item);
            if (parent != null) {
                parentPos = fastAdapter.getPosition(parent);
                boolean success = ((IExpandable) parent).getSubItems().remove(item);
                Log.d("DELETE", "success=" + success + " | deletedId=" + item.getIdentifier() + " | parentId=" + parent.getIdentifier() + " (sub items: " + ((IExpandable) parent).getSubItems().size() + ") | parentPos=" + parentPos);

                // check if parent is expanded and notify the adapter about the removed item, if necessary (only if parent is visible)
                if (parentPos != -1 && ((IExpandable) parent).isExpanded()) {
                    fastAdapter.notifyAdapterSubItemsChanged(parentPos, ((IExpandable) parent).getSubItems().size() + 1);
                }

                // if desired, notify the parent about it's changed items (only if parent is visible!)
                if (parentPos != -1 && notifyParent) {
                    expanded = ((IExpandable) parent).isExpanded();
                    fastAdapter.notifyAdapterItemChanged(parentPos);
                    // expand the item again if it was expanded before calling notifyAdapterItemChanged
                    if (expanded) {
                        fastAdapter.expand(parentPos);
                    }
                }

                deleted.add(item);

                if (deleteEmptyHeaders && ((IExpandable) parent).getSubItems().size() == 0) {
                    it.add(parent);
                    it.previous();
                }
            } else if (pos != -1) {
                // if we did not find a parent, we remove the item from the adapter
                IAdapter adapter = fastAdapter.getAdapter(pos);
                boolean success = false;
                if (adapter instanceof IItemAdapter) {
                    success = ((IItemAdapter) adapter).remove(pos) != null;
                }
                boolean isHeader = item instanceof IExpandable && ((IExpandable) item).getSubItems() != null;
                Log.d("DELETE", "success=" + success + " | deletedId=" + item.getIdentifier() + "(" + (isHeader ? "EMPTY HEADER" : "ITEM WITHOUT HEADER") + ")");
                deleted.add(item);
            }
        }

        Log.d("DELETE", "deleted (incl. empty headers): " + deleted.size());

        return deleted;
    }

    public interface IPredicate<T> {
        boolean apply(T data);
    }
}