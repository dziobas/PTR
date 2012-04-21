package pl.araneo.ptr;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;


public class PolTabRej extends Activity {
    private PtrDbAdapter mDbHelper;
    Cursor mRowCursor;
    private final int PREFERENCES = Menu.FIRST;
    private final int ABOUT_ID    = Menu.FIRST + 1;

    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        mDbHelper = new PtrDbAdapter(this);
        mDbHelper.open();
        mRowCursor = mDbHelper.fetchAllRows();
        startManagingCursor(mRowCursor);

        final RowCursorAdapter adapt = new RowCursorAdapter(this, mRowCursor);

        ListView list                = (ListView) findViewById(R.id.listView);
        list.setAdapter(adapt);

        EditText edit = (EditText) findViewById(R.id.editText);
        edit.addTextChangedListener(
            new TextWatcher() {
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    adapt.changeCursor(adapt.runQueryOnBackgroundThread(s));
                }

                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                public void afterTextChanged(Editable s) {
                }
            });

        InputFilter filter = new InputFilter() {
                public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                    for(int i = start; i < end; i++) {
                        if(!(Character.isLetterOrDigit(source.charAt(i)) || (source.charAt(i) == ' ') || (source.charAt(i)=='-'))) {
                            return "";
                        } 
                    }

                    return null;
                }
            };

        edit.setFilters(new InputFilter[]{ new InputFilter.AllCaps(), filter });

        registerForContextMenu(list);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            // handles a click on a search suggestion; launches activity to show word
//            Intent wordIntent = new Intent(this, WordActivity.class);
//            wordIntent.setData(intent.getData());
//            startActivity(wordIntent);
//            finish();
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            // handles a search query
            String query = intent.getStringExtra(SearchManager.QUERY);
            EditText edit = (EditText) findViewById(R.id.editText);
            edit.setText(query);
//            showResults(query);
        }
    }
    
    protected void onDestroy() {
        super.onDestroy();
        mDbHelper.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRowCursor.requery();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, PREFERENCES, Menu.NONE, "Ustawienia");
        menu.add(Menu.NONE, ABOUT_ID, Menu.NONE, "O programie");

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case ABOUT_ID:

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                View view                   = LayoutInflater.from(this).inflate(R.layout.about, null);
                builder.setView(view);
                builder.setTitle(R.string.about);
                builder.setIcon(android.R.drawable.ic_dialog_info);
                builder.setPositiveButton(
                    "Ok",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });


                builder.create().show();

                return super.onOptionsItemSelected(item);

            case PREFERENCES:
                startActivity(new Intent(this, CheckBoxPreferenceActivity.class));
                mRowCursor.requery();

                return true;

            default:
                return false;
        }
    }

    class RowCursorAdapter extends CursorAdapter implements Filterable {
        private PtrDbAdapter mDb;

        public RowCursorAdapter(Context context, Cursor c) {
            super(context, c);
            mDb = new PtrDbAdapter(context);
            mDb.open();
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View row                = inflater.inflate(R.layout.row, null);

            ViewWrapper wrapper     = new ViewWrapper(row);
            row.setTag(wrapper);
            wrapper.update(cursor);

            return row;
        }

        public void bindView(View view, Context context, Cursor cursor) {
            ViewWrapper wrapper = (ViewWrapper) view.getTag();
            wrapper.update(cursor);
        }

        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            return mDb.query(constraint.toString());
        }
    }

    class ViewWrapper {
        View view;
        TextView powiat      = null;
        TextView wojewodztwo = null;
        TextView tablica     = null;

        public ViewWrapper(View view) {
            this.view = view;
        }

        public TextView getPowiat() {
            if(powiat == null) {
                powiat = (TextView) view.findViewById(R.id.textPowiat);
            }

            return powiat;
        }

        public TextView getWojewodztwo() {
            if(wojewodztwo == null) {
                wojewodztwo = (TextView) view.findViewById(R.id.textWojewodztwo);
            }

            return wojewodztwo;
        }

        public TextView getTablica() {
            if(tablica == null) {
                tablica = (TextView) view.findViewById(R.id.textTablica);
            }

            return tablica;
        }

        public void update(Cursor c) {
            getPowiat().setText(c.getString(c.getColumnIndex(PtrDbAdapter.KEY_NAME)));
            getWojewodztwo().setText(c.getString(c.getColumnIndex(PtrDbAdapter.KEY_DESCRIPTION)));
            getTablica().setText(c.getString(c.getColumnIndex(PtrDbAdapter.KEY_SHORT)));
        }
    }
}
