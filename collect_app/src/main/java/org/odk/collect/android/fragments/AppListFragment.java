/*

Copyright 2017 Shobhit
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.odk.collect.android.fragments;

import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.database.ActivityLogger;
import org.odk.collect.android.provider.InstanceProviderAPI;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;


abstract class AppListFragment extends ListFragment {
    private static final int MENU_SORT = Menu.FIRST;
    private static final int MENU_FILTER = MENU_SORT + 1;

    protected final ActivityLogger logger = Collect.getInstance().getActivityLogger();
    protected String[] mSortingOptions;
    View rootView;
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    protected LinearLayout mSearchBoxLayout;
    protected SimpleCursorAdapter mListAdapter;
    protected LinkedHashSet<Long> mSelectedInstances = new LinkedHashSet<>();
    protected EditText mInputSearch;

    // toggles to all checked or all unchecked
    // returns:
    // true if result is all checked
    // false if result is all unchecked
    //
    // Toggle behavior is as follows:
    // if ANY items are unchecked, check them all
    // if ALL items are checked, uncheck them all
    public static boolean toggleChecked(ListView lv) {
        // shortcut null case
        if (lv == null) return false;

        boolean newCheckState = lv.getCount() > lv.getCheckedItemCount();
        setAllToCheckedState(lv, newCheckState);
        return newCheckState;
    }

    public static void setAllToCheckedState(ListView lv, boolean check) {
        // no-op if ListView null
        if (lv == null) {
            return;
        }
        for (int x = 0; x < lv.getCount(); x++) {
            lv.setItemChecked(x, check);
        }
    }

    // Function to toggle button label
    public static void toggleButtonLabel(Button mToggleButton, ListView lv) {
        if (lv.getCheckedItemCount() != lv.getCount()) {
            mToggleButton.setText(R.string.select_all);
        } else {
            mToggleButton.setText(R.string.clear_all);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (!isVisibleToUser) {
            // close the drawer if open
            if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.END)) {
                mDrawerLayout.closeDrawer(Gravity.END);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Collect.getInstance().getActivityLogger().logInstanceAction(this, "onCreateOptionsMenu", "show");
        super.onCreateOptionsMenu(menu, inflater);

        menu
                .add(0, MENU_SORT, 0, R.string.sort_the_list)
                .setIcon(R.drawable.ic_sort)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        menu
                .add(0, MENU_FILTER, 0, R.string.filter_the_list)
                .setIcon(R.drawable.ic_search)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SORT:
                if (mDrawerLayout.isDrawerOpen(Gravity.END)) {
                    mDrawerLayout.closeDrawer(Gravity.END);
                } else {
                    Collect.getInstance().hideKeyboard(mInputSearch);
                    mDrawerLayout.openDrawer(Gravity.END);
                }
                return true;

            case MENU_FILTER:
                if (mSearchBoxLayout.getVisibility() == View.GONE) {
                    showSearchBox();
                } else {
                    hideSearchBox();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    private void setupDrawerItems() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_list_item_1, mSortingOptions) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setPadding(50, 0, 0, 0);
                return textView;
            }
        };
        mDrawerList.setAdapter(adapter);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                performSelectedSearch(position);
                mDrawerLayout.closeDrawer(Gravity.END);
            }
        });
    }

    private void performSelectedSearch(int position) {
        hideSearchBox();
        switch (position) {
            case 0:
                sortByNameAsc();
                break;
            case 1:
                sortByNameDesc();
                break;
            case 2:
                sortByDateDesc();
                break;
            case 3:
                sortByDateAsc();
                break;
            case 4:
                sortByStatusAsc();
                break;
            case 5:
                sortByStatusDesc();
                break;
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDrawer(view);
        setupDrawerItems();
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    private void setupDrawer(View rootView) {
        mDrawerList = (ListView) rootView.findViewById(R.id.sortingMenu);
        mDrawerLayout = (DrawerLayout) rootView.findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mDrawerToggle = new ActionBarDrawerToggle
                (getActivity(), mDrawerLayout,
                        R.string.sorting_menu_open, R.string.sorting_menu_close) {
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActivity().invalidateOptionsMenu();
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getActivity().invalidateOptionsMenu();
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }
        };

        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    protected void checkPreviouslyCheckedItems() {
        getListView().clearChoices();
        List<Integer> selectedPositions = new ArrayList<>();
        int listViewPosition = 0;
        Cursor cursor = mListAdapter.getCursor();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long instanceId = cursor.getLong(cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns._ID));
                if (mSelectedInstances.contains(instanceId)) {
                    selectedPositions.add(listViewPosition);
                }
                listViewPosition++;
            } while (cursor.moveToNext());
        }

        for (int position : selectedPositions) {
            getListView().setItemChecked(position, true);
        }
    }

    private void hideSearchBox() {
        mInputSearch.setText("");
        mSearchBoxLayout.setVisibility(View.GONE);
        Collect.getInstance().hideKeyboard(mInputSearch);
    }

    private void showSearchBox() {
        mSearchBoxLayout.setVisibility(View.VISIBLE);
        Collect.getInstance().showKeyboard(mInputSearch);
    }

    protected abstract void filter(CharSequence charSequence);

    protected abstract void sortByNameAsc();

    protected abstract void sortByNameDesc();

    protected abstract void sortByDateAsc();

    protected abstract void sortByDateDesc();

    protected abstract void sortByStatusAsc();

    protected abstract void sortByStatusDesc();

    protected abstract void setupAdapter(String sortOrder);

    protected boolean areCheckedItems() {
        return getCheckedCount() > 0;
    }

    /**
     * Returns the IDs of the checked items, using the ListView of this activity.
     */
    protected long[] getCheckedIds() {
        return getCheckedIds(getListView());
    }

    /**
     * Returns the IDs of the checked items, using the ListView provided
     */
    protected long[] getCheckedIds(ListView lv) {
        // This method could be simplified by using getCheckedItemIds, if one ensured that
        // IDs were “stable” (see the getCheckedItemIds doc).
        int itemCount = lv.getCount();
        int checkedItemCount = lv.getCheckedItemCount();
        long[] checkedIds = new long[checkedItemCount];
        int resultIndex = 0;
        for (int posIdx = 0; posIdx < itemCount; posIdx++) {
            if (lv.isItemChecked(posIdx)) {
                checkedIds[resultIndex] = lv.getItemIdAtPosition(posIdx);
                resultIndex++;
            }
        }
        return checkedIds;
    }

    /**
     * Returns the IDs of the checked items, as an array of Long
     */
    protected Long[] getCheckedIdObjects() {
        long[] checkedIds = getCheckedIds();
        Long[] checkedIdObjects = new Long[checkedIds.length];
        for (int i = 0; i < checkedIds.length; i++) {
            checkedIdObjects[i] = checkedIds[i];
        }
        return checkedIdObjects;
    }

    protected int getCheckedCount() {
        return getListView().getCheckedItemCount();
    }
}
