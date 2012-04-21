package pl.araneo.ptr;

import android.content.SearchRecentSuggestionsProvider;


public class PlatesRecentSuggestionsProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = "pl.araneo.ptr.PlatesRecentSuggestionsProvider";
    public final static int    MODE      = DATABASE_MODE_QUERIES;

    public PlatesRecentSuggestionsProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }
}
