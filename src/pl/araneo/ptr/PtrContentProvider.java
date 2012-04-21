package pl.araneo.ptr;

import android.content.SearchRecentSuggestionsProvider;

import android.app.SearchManager;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;
import java.util.HashMap;
import pl.araneo.ptr.PtrDbAdapter.DatabaseHelper;

public class PtrContentProvider extends SearchRecentSuggestionsProvider{
	private static final String TAG = "PtrContentProvider"; 
	// Uri matcher to decode incoming URIs.
    private final UriMatcher mUriMatcher;
    
 // A projection map used to select columns from the database
    private final HashMap<String, String> mNotesProjectionMap;
    
 // The incoming URI matches the main table URI pattern
    private static final int MAIN = 1;
    
    
	
	  public PtrContentProvider() {
		  mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
//		  mUriMatcher.addURI(AUTHORITY, MainTable.TABLE_NAME, MAIN);
		  mUriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, MAIN);
		  mUriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY+"/*", MAIN);
		  
		  mNotesProjectionMap = new HashMap<String, String>();
          mNotesProjectionMap.put(MainTable._ID, MainTable._ID);
          mNotesProjectionMap.put(SearchManager.SUGGEST_COLUMN_TEXT_1, MainTable.COLUMN_NAME_NAME);
          mNotesProjectionMap.put(SearchManager.SUGGEST_COLUMN_TEXT_2, MainTable.COLUMN_NAME_DESCRIPTION);
          mNotesProjectionMap.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
//          mNotesProjectionMap.put(MainTable.COLUMN_NAME_NAME, MainTable.COLUMN_NAME_NAME);
//          mNotesProjectionMap.put(MainTable.COLUMN_NAME_DESCRIPTION, MainTable.COLUMN_NAME_DESCRIPTION);
//          mNotesProjectionMap.put(MainTable.COLUMN_NAME_SHORT, MainTable.COLUMN_NAME_SHORT);
	  }


	/**
     * The authority to provider.
     */
    public static final String AUTHORITY = "pl.araneo.ptr.provider.PtrContentProvider";


	@Override
	public String getType(Uri uri) {
		return SearchManager.SHORTCUT_MIME_TYPE;
	}

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

	private DatabaseHelper mOpenHelper;
	 /**
     * Perform provider creation.
     */
    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        // Assumes that any failures will be reported by a thrown exception.
        return true;
    }

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Log.d(TAG, "uri "+uri.getPath());
        // Constructs a new query builder and sets its table name
        SQLiteQueryBuilder qbName = new SQLiteQueryBuilder();
        SQLiteQueryBuilder qbPrefix = new SQLiteQueryBuilder();
//        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qbName.setTables(MainTable.TABLE_NAME);
        qbPrefix.setTables(MainTable.TABLE_NAME);
        
        switch (mUriMatcher.match(uri)) {
            case MAIN:
                // If the incoming URI is for main table.
                qbName.setProjectionMap(mNotesProjectionMap);
                qbPrefix.setProjectionMap(mNotesProjectionMap);

                String key = uri.getLastPathSegment();
                Log.d(TAG, "Query: [" + key + "]");
        		String prefixWhere, nameWhere;
        		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        		if (pref.getBoolean("query_tablice", false)) {
        			Log.d(TAG, "query_tablice true");
        			prefixWhere = buildQueryPrefix(key);
        		} else {
        			Log.d(TAG, "query_tablice false");
        			prefixWhere = " 0 ";
        		}
        		if (pref.getBoolean("query_nazwa", false)) {
        			Log.d(TAG, "query_nazwa true");
        			nameWhere = buildQueryName(key);
        		} else {
        			Log.d(TAG, "query_nazwa false");
        			nameWhere = " 0 ";
        		}
                
                qbName.appendWhere(nameWhere);
                qbPrefix.appendWhere(prefixWhere);
//                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs,
//                        new String[] { uri.getLastPathSegment() });
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        
        
        

//        if (TextUtils.isEmpty(sortOrder)) {
//            sortOrder = MainTable.DEFAULT_SORT_ORDER;
//        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        String[] proj = new String[]{"_id", MainTable.COLUMN_NAME_SHORT+" || ' ' || "+MainTable.COLUMN_NAME_NAME+" AS "+ SearchManager.SUGGEST_COLUMN_TEXT_1, MainTable.COLUMN_NAME_DESCRIPTION+" AS "+ SearchManager.SUGGEST_COLUMN_TEXT_2, "\""+SearchManager.SUGGEST_NEVER_MAKE_SHORTCUT+ "\" AS "+ SearchManager.SUGGEST_COLUMN_SHORTCUT_ID};
        Cursor cName = qbName.query(db, proj, null, null,
                null /* no group */, null /* no filter */, null);
        Log.d(TAG, "name count "+cName.getCount());
        Cursor cPrefix = qbPrefix.query(db, proj, null, null,
        		null /* no group */, null /* no filter */, null);
        Log.d(TAG, "prefix count "+cPrefix.getCount());

        MergeCursor c = new MergeCursor(new Cursor[]{ cPrefix, cName});
        Log.d(TAG, "merge count "+c.getCount());
        
        c.setNotificationUri(getContext().getContentResolver(), uri);

        for(int i=0;i<c.getColumnCount();i++){
        	Log.d(TAG, "column "+i+" "+c.getColumnName(i));
        }
        while(c.moveToNext()){
        	String s = "";
        	for(int i=0;i<c.getColumnCount();i++){
        		s+=c.getString(i)+", ";
        	}
        	Log.d(TAG, s);
        }
        
        
        return c;
	}

	
    public static final class MainTable implements BaseColumns {

        // This class cannot be instantiated
        private MainTable() {}

        /**
         * The table name offered by this provider
         */
        public static final String TABLE_NAME = "tablice";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI =  Uri.parse("content://" + AUTHORITY + "/tablice");

        /**
         * The content URI base for a single row of data. Callers must
         * append a numeric row id to this Uri to retrieve a row
         */
        public static final Uri CONTENT_ID_URI_BASE
                = Uri.parse("content://" + AUTHORITY + "/tablice/");

        /**
         * The MIME type of {@link #CONTENT_URI}.
         */
        public static final String CONTENT_TYPE
                = "vnd.android.cursor.dir/vnd.araneo.plates";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single row.
         */
        public static final String CONTENT_ITEM_TYPE
                = "vnd.android.cursor.item/vnd.araneo.plates";
        /**
         * The default sort order for this table
         */
//        public static final String DEFAULT_SORT_ORDER = "data COLLATE LOCALIZED ASC";

        /**
         * Column name for the single column holding our data.
         * <P>Type: TEXT</P>
         */
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_SHORT = "short";
        public static final String COLUMN_NAME_DESCRIPTION = "description";
    }

	/*
	 * prefix NOT LIKE 'key%' AND ( name LIKE 'key%' OR name LIKE '% key%' )
	 * hack to bypass problem with different cases of special characters in
	 * sqlite3 which are not supported: prefix NOT LIKE 'key%' AND ( name LIKE
	 * 'key%' OR name LIKE '% key%' OR name LIKE 'sKey%' OR name LIKE '% sKey%')
	 */
	private String buildQueryName(String key) {
		String _key = key.toLowerCase();
		StringBuilder str = new StringBuilder(" ");
		str.append(PtrDbAdapter.KEY_PREFIX).append(" NOT LIKE '").append(_key).append("%' AND (");
		str.append(PtrDbAdapter.KEY_NAME).append(" LIKE '").append(_key).append("%' OR ");
		str.append(PtrDbAdapter.KEY_NAME).append(" LIKE '% ").append(_key).append("%' ");
		boolean special = false;
		StringBuilder sKey = new StringBuilder();
		if (_key != null && _key.length() > 0) {
			final String SPACE = " "; 
			// tab is array of words
			String[] tab = _key.split(" ");
			for (int i = 0; i < tab.length; i++) {
				if (tab[i].length() == 0) {
					continue;
				}
				// check only first letters of word
				char c = tab[i].charAt(0);
				/*
				 * � � � � � = 0142 015B 017C 017A 0107 � � � � � = 0141 015A
				 * 017B 0179 0106
				 */
				if (c == '\u0142' || c == '\u015B' || c == '\u017C' || c == '\u017A' || c == '\u0107') {
					// to upper case
					c = Character.toUpperCase(c);
					if (sKey.length() > 0) {
						sKey.append(SPACE).append(c).append(tab[i].substring(1));
					} else {
						sKey.append(c).append(tab[i].substring(1));
					}
					special = true;
				} else {
					if (sKey.length() == 0) {
						sKey.append(tab[i]);
					} else {
						sKey.append(SPACE).append(tab[i]);
					}
				}
			}
		}

		if (special) {
			// when special characters available
			str.append(" OR ").append(PtrDbAdapter.KEY_NAME).append(" LIKE '").append(sKey).append("%' OR ");
			str.append(PtrDbAdapter.KEY_NAME).append(" LIKE '% ").append(sKey).append("%' ) ");
		} else {
			str.append(") ");
		}
//		Log.d(TAG, str.toString());
		return str.toString();
	}

	/*
	 * prefix LIKE 'key%' OR ('key' GLOB pattern AND('key' NOT LIKE 'WW%' AND
	 * 'key' NOT LIKE 'WX%')) OR ('key' GLOB prefix||'[0-9]*' AND prefix='WW'
	 * AND (length('key')<7 OR 'key' GLOB pattern AND 'key' LIKE '_______')) OR
	 * ('key' GLOB prefix||'[0-9]*' AND prefix='WX' AND ((length('key')<7 OR
	 * 'key' GLOB pattern AND 'key' LIKE '_______') OR ('key' GLOB pattern AND
	 * 'key' NOT LIKE 'WX____Y')))
	 */
	private String buildQueryPrefix(String key) {
		if (key.length() > 8) {
			return " 0 ";
		}
		StringBuilder str = new StringBuilder(" ");
		str.append(PtrDbAdapter.KEY_PREFIX).append(" LIKE '").append(key).append("%' OR ('").append(key);
		str.append("' GLOB ").append(PtrDbAdapter.KEY_PATTERN).append(" AND ('").append(key);
		str.append("' NOT LIKE 'WW%' AND '").append(key).append("' NOT LIKE 'WX%')) ");
		str.append(" OR ('").append(key).append("' GLOB ").append(PtrDbAdapter.KEY_PREFIX).append("||'[0-9]*' AND ");
		str.append(PtrDbAdapter.KEY_PREFIX).append("='WW' AND   (length('").append(key).append("')<7 OR '");
		str.append(key).append("' GLOB ").append(PtrDbAdapter.KEY_PATTERN).append(" AND '").append(key);
		str.append("' LIKE '_______')) ").append("OR ('").append(key).append("' GLOB ");
		str.append(PtrDbAdapter.KEY_PREFIX).append("||'[0-9]*' AND ").append(PtrDbAdapter.KEY_PREFIX);
		str.append("='WX' AND ((length('").append(key).append("')<7 OR '").append(key).append("' GLOB ");
		str.append(PtrDbAdapter.KEY_PATTERN).append(" AND '").append(key).append("' LIKE '_______') OR ('");
		str.append(key).append("' GLOB ").append(PtrDbAdapter.KEY_PATTERN).append(" AND '");
		str.append(key).append("' NOT LIKE 'WX____Y'))) ");
		return str.toString();
	}
}
