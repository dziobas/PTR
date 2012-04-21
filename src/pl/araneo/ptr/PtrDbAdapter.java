package pl.araneo.ptr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MergeCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;

public class PtrDbAdapter {

	public static final String KEY_ROWID = "_id";
	public static final String KEY_PREFIX = "prefix";
	public static final String KEY_PATTERN = "pattern";
	public static final String KEY_NAME = "name";
	public static final String KEY_SHORT = "short";
	public static final String KEY_DESCRIPTION = "description";

	private static final String DATABASE_NAME = "ptr";
	private static final String DATABASE_TABLE = "tablice";
	private static final int DATABASE_VERSION = 11;
	private static final String TAG = "PtrDbAdapter";

	private static final String DATABASE_CREATE = "CREATE TABLE " + DATABASE_TABLE + " (" + KEY_ROWID
			+ " integer primary key autoincrement, " + KEY_PREFIX + " text not null, " + KEY_PATTERN
			+ " text not null, " + KEY_SHORT + " text not null, " + KEY_NAME + " text not null, " + KEY_DESCRIPTION
			+ " text not null);";

	private static final String INSERT = "INSERT INTO tablice (prefix, pattern, short, name, description) VALUES (";
	private final Context mCtx;
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	public static class DatabaseHelper extends SQLiteOpenHelper {
		private Context mCtx;

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mCtx = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d(TAG, "Tworzenie bazy danych");
			db.execSQL(DATABASE_CREATE);

			try {
				InputStream in = mCtx.getResources().openRawResource(R.raw.data);
				if (in != null) {
					InputStreamReader tmp = new InputStreamReader(in);
					BufferedReader reader = new BufferedReader(tmp);
					String str;
					while ((str = reader.readLine()) != null) {
						db.execSQL(INSERT + str);
					}
					in.close();
				}
			} catch (java.io.FileNotFoundException e) {
			} catch (IOException e) {
				Log.d(TAG, "IOException ");
			}

			Log.d(TAG, "Utworzono baze danych");

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
					+ ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			onCreate(db);
		}
	}

	public Cursor query(String key) {
		Log.d(TAG, "Query: [" + key + "]");
		String where;
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mCtx);
		Cursor[] cursors = new Cursor[2];
		if (pref.getBoolean("query_tablice", false)) {
			Log.d(TAG, "query_tablice true");
			where = buildQueryPrefix(key);
			cursors[0] = mDb.query(DATABASE_TABLE, new String[]{KEY_ROWID, KEY_NAME, KEY_DESCRIPTION, KEY_SHORT},
					where, null, null, null, null);
		} else {
			Log.d(TAG, "query_tablice false");
			cursors[0] = mDb.query(DATABASE_TABLE, new String[]{KEY_ROWID, KEY_NAME, KEY_DESCRIPTION, KEY_SHORT},
					" 0 ", null, null, null, null);
		}
		if (pref.getBoolean("query_nazwa", false)) {
			Log.d(TAG, "query_nazwa true");
			where = buildQueryName(key);
			cursors[1] = mDb.query(true, DATABASE_TABLE, new String[]{KEY_ROWID, KEY_NAME, KEY_DESCRIPTION, KEY_SHORT},
					where, null, null, null, null, null);
		} else {
			Log.d(TAG, "query_nazwa false");
			cursors[1] = mDb.query(DATABASE_TABLE, new String[]{KEY_ROWID, KEY_NAME, KEY_DESCRIPTION, KEY_SHORT},
					" 0 ", null, null, null, null);
		}
		MergeCursor cursor = new MergeCursor(cursors);

		return cursor;
	}

	public PtrDbAdapter(Context ctx) {
		this.mCtx = ctx;
	}

	public PtrDbAdapter open() throws SQLException {
		Log.d(TAG, "open in");
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		Log.d(TAG, "open out");
		return this;
	}

	public Cursor fetchAllRows() {
		return mDb.query(DATABASE_TABLE, new String[]{KEY_ROWID, KEY_SHORT, KEY_NAME, KEY_DESCRIPTION}, null, null,
				null, null, null);
	}

	public void close() {
		mDbHelper.close();
	}

	static final char[] specialCharacters = {'�', '�', '�', '�'};

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
