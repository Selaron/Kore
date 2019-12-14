package org.xbmc.kore.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.xbmc.kore.R;
import org.xbmc.kore.jsonrpc.event.MediaSyncEvent;

import de.greenrobot.event.EventBus;

public abstract class AbstractSearchableFragment extends Fragment implements SearchView.OnQueryTextListener {

    private String searchFilter = null;
    private String savedSearchFilter;
    private boolean supportsSearch;
    private boolean isPaused;

    private EventBus bus;
    private SearchView searchView;

    private final String BUNDLE_KEY_SEARCH_QUERY = "search_query";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        bus = EventBus.getDefault();

        if (savedInstanceState != null) {
            savedSearchFilter = savedInstanceState.getString(BUNDLE_KEY_SEARCH_QUERY);
        }
        searchFilter = savedSearchFilter;

        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.abstractcursorlistfragment, menu);

        if (supportsSearch) {
            setupSearchMenuItem(menu, inflater);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Use this to indicate your fragment supports search queries.
     * Get the entered search query using {@link #getSearchFilter()}
     * <br/>
     * Note: make sure this is set before {@link #onCreateOptionsMenu(Menu, MenuInflater)} is called.
     * For instance in {@link #onAttach(Activity)}
     *
     * @param supportsSearch true if you support search queries, false otherwise
     */
    public void setSupportsSearch(boolean supportsSearch) {
        this.supportsSearch = supportsSearch;
    }

    /**
     * Save the search state of the list fragment
     */
    public void saveSearchState() {
        savedSearchFilter = searchFilter;
    }

    /**
     * @return text entered in searchview
     */
    public String getSearchFilter() {
        return searchFilter;
    }

    private void setupSearchMenuItem(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.media_search, menu);
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        if (searchMenuItem != null) {
            searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
            searchView.setOnQueryTextListener(this);
            searchView.setQueryHint(getString(R.string.action_search));
            if (!TextUtils.isEmpty(savedSearchFilter)) {
                searchMenuItem.expandActionView();
                searchView.setQuery(savedSearchFilter, false);
                //noinspection RestrictedApi
                searchView.clearFocus();
            }

            MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    searchFilter = savedSearchFilter = null;
                    restartLoader();
                    return true;
                }
            });
        }

        //Handle clearing search query using the close button (X button).
        View view = searchView.findViewById(R.id.search_close_btn);
        if (view != null) {
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText editText = (EditText) searchView.findViewById(R.id.search_src_text);
                    editText.setText("");
                    searchView.setQuery("", false);
                    searchFilter = savedSearchFilter = "";
                    restartLoader();
                }
            });
        }
    }


    /**
     * Search view callbacks
     */
    /** {@inheritDoc} */
    @Override
    public boolean onQueryTextChange(String newText) {
        if ((!searchView.hasFocus()) && TextUtils.isEmpty(newText)) {
            //onQueryTextChange called as a result of manually expanding the searchView in setupSearchMenuItem(...)
            return true;
        }

        /**
         * When this fragment is paused, onQueryTextChange is called with an empty string.
         * This causes problems restoring the list fragment when returning. On return the fragment
         * is recreated, which will cause the cursor adapter to be recreated. Although
         * the search view will have the query restored to its saved state, the CursorAdapter
         * will use the empty search filter. This is due to the fact that we don't restart the
         * loader when it is still loading after its been created.
         */
        if (isPaused)
            return true;

        searchFilter = newText;

        restartLoader();

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean onQueryTextSubmit(String newText) {
        // All is handled in onQueryTextChange
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_refresh:
                restartLoader();
                break;
        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onResume() {
 //       bus.register(this);
        super.onResume();
        isPaused = false;
    }

    @Override
    public void onPause() {
//        bus.unregister(this);
        super.onPause();
        isPaused = true;
    }

    /**
     * Event bus post. Called when the syncing process ended
     *
     * @param event Refreshes data
     */
    public void onEventMainThread(MediaSyncEvent event) {
        onSyncProcessEnded(event);
    }

    protected void onSyncProcessEnded(MediaSyncEvent event) {

    }

    protected abstract void restartLoader();
}