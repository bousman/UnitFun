package com.project.bousman.unitfun;

/**
 * @file
 * @brief This file implements the Unit activity of the app to display the unit elements
 *        like Inch, Foot, etc. and perform the conversion as user types new values.
 *
 * All of the unit data is passed in to the Intent as a Bundle using the extras.
 *
 * @see Unit
 */

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;


/**
 * The UnitActivity class handles displaying a list of units (e.g foot, inch).
 * It uses a ListView with custom adapter to show the unit name, current value,
 * and an informational button to get a longer description of the unit.
 */
public class UnitActivity extends AppCompatActivity implements SoundPool.OnLoadCompleteListener {

    //! Obviously need a custom adapter for the ListView to display each unit item
    private MyCustomAdapter dataAdapter = null;
    //! how many of the "reference value" is in play right now?  Every displayed unit
    //! will be calculated from this using the scale factor each unit has
    private BigDecimal mReferenceValue = new BigDecimal("0.0");
    //! if mRefValueEmpty is true then no text is displayed in the units EditText field
    //! when user types a value this is turned off and all units can be updated
    private boolean mRefValueEmpty = true;
    //! the units to display - title, information, and scale factor
    private ArrayList<Unit> mUnitList;
    // will get set to the name of the reference unit
    private String mRefUnitName = "none";

    private String mCurrentTitle = null;
    private static Bundle mSavedBundle = new Bundle();
    private static boolean mRestoreState = false;

    // for sound effect when new values entered by user
    private int mSoundId1;
    private static SoundPool mSoundPool;
    private boolean mPlaySound;

    private final OkHttpClient mOkHttpClient;

    public UnitActivity() {
        mOkHttpClient = new OkHttpClient();
    }


    /**
     * Calculate new master reference value from this new unit value
     *
     * All units have a conversion factor to get from the master reference
     * to their own value. Sets member variable mReferenceValue equal to
     * new reference and also sets mRefValueEmpty to false now there is a
     * reference value set.
     *
     * @param editedUnit     the 'Unit' object that was just edited
     * @param numberString   the new value for that Unit, expressed as a string
     */
    public void updRefValue(Unit editedUnit, String numberString) {
        //
        // i.e. if Kilometer now has new value go back and calculate new value for
        // reference value, Meter, so that all units can be recalculated from master.
        // Note: this routine tells the data adapter that the data set has changed
        //       using notifyDataSetChanged()
        if ( "".equals(numberString) )
            return;

        Boolean goodNumber = false;
        Double editValue = 0.;
        try {
            editValue = Double.valueOf(numberString);
            goodNumber = true;
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        if (goodNumber) {
            if (mPlaySound)
                mSoundPool.play(mSoundId1, 1, 1, 0, 0, 1);

            BigDecimal editValueBD = new BigDecimal(editValue);
            mReferenceValue = editedUnit.calculateReference(editValueBD);
            mRefValueEmpty = false;  // allow values to be put in unit EditText boxes

            // this will cause Android to update all the widgets on the screen
            // and thus each EditView will recalculate the unit value from the
            // new reference value
            dataAdapter.notifyDataSetChanged();
        }
    }


    /**
     *  The saved state bundle may have the current reference value - good for
     *  when the screen has rotated.  If not found it will use a default.  This
     *  will be looked for with the key "reference".
     *
     *  The Intent should have a Bundle in the "extras" containing the units
     *  to display and convert.  e.g. Length with meter, inch, foot or Mass
     *  with pound, kilogram, etc.  The following keys are expected:
     *      unit_title          the main title for the page (e.g. "Length")
     *      unit_data           ArrayList<Unit> of all the units (it's a parcelable object)
     *      reference           reference value the last time this page was used (0. if not)
     *      reference_empty     if true consider no reference value and leave unit EditText empty
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unit);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickSnack(view);
            }
        });

        // get "settings" value for keeping unit values.  If true then will try to set
        // the reference value to what it was when we were last here
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean keep_values = settings.getBoolean(getString(R.string.pref_retain), false);
        mPlaySound = settings.getBoolean(getString(R.string.pref_sound), false);


        // reference value from last time at this particular UnitActivity is passed in
        // (which will be zero on first time or if EditText boxes are empty)
        Bundle b = getIntent().getExtras();
        if ( b == null ) {
            Log.d("UnitActivity onCreate","empty bundle so get from saved state");
            b = mSavedBundle;
            if ( b == null ) {
                Log.d("onCreate", "bundle still empty");
                Log.d("onCreate", "bundle: "+b.toString());
            }
        }

        // the reference value is part of the saved state so when the screen
        // rotates we won't lose our values
        if ( savedInstanceState != null ) {
            Log.d("UnitActivity onCreate","savedInstanceState");
            try {
                double d = savedInstanceState.getDouble("reference");
                mReferenceValue = new BigDecimal(d);
                mRefValueEmpty = savedInstanceState.getBoolean("reference_empty");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.d("UnitActivity onCreate","no saved state");
            // if the "keep values" setting is turned on then use the reference value
            // from the previous call to this activity that is being passed back through
            // the Bundle
            if ( keep_values || mRestoreState ) {
                mReferenceValue = new BigDecimal(b.getDouble("reference"));
                mRefValueEmpty = b.getBoolean("reference_empty");
                mRestoreState = false;
            } /*else {
                // This is first time into this activity or the "keep values" setting is off
                // so use default values
            } */
        }

        if (Global.mRefValueSet) {
            mReferenceValue = new BigDecimal(Global.mNewRefValue);
            mRefValueEmpty = false;
            //Log.d("UnitActivity","Set new ref = " + mReferenceValue.toString());
            Global.mRefValueSet = false;
        }

        mCurrentTitle = b.getString("unit_title");   // page title

        // up/back in the toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.app_bar);
        myToolbar.setTitle(mCurrentTitle);
        setSupportActionBar(myToolbar);
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }


        // get the measurements for this unit (e.g. foot, inch) and pass them off
        // to get up the main display
        ArrayList<Unit> unitList = b.getParcelableArrayList("unit_data");

        // get name of reference unit.  currently the only way to do this is look for the one
        // that has a scale factor of 1.0
        if (unitList != null) {
            for (int ii = 0; ii < unitList.size(); ++ii) {
                if (unitList.get(ii).getScale().floatValue() == 1) {
                    mRefUnitName = unitList.get(ii).getTitle();
                    break;
                }
            }
        }

        // create the display with all the units
        displayListView(unitList);

        mSoundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        mSoundId1 = mSoundPool.load(this, R.raw.grunz_success_low, 1);
        mSoundPool.setOnLoadCompleteListener(this);
    }


    @Override
    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
        //soundPool.play(sampleId, 1, 1, 0, 0, 1);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Toast.makeText(this, "onCreateOptionsMenu", Toast.LENGTH_LONG).show();

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.unit_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mSavedBundle.clear();
        createSavedState(mSavedBundle);
        mRestoreState = true;

        switch (item.getItemId()) {
            case R.id.action_facebook:
                Log.d("onOptions","facebook");
                Intent intent = new Intent(UnitActivity.this, FacebookActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_random:
                Log.d("onOptions","random");
                Intent intentran = new Intent(UnitActivity.this, SensorActivity.class);
                startActivityForResult(intentran, SENSOR_REQUEST_CODE);
                //startActivity(intentran);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }


    public void createSavedState(Bundle savedState) {
        savedState.putDouble("reference",mReferenceValue.doubleValue());
        savedState.putBoolean("reference_empty", mRefValueEmpty);
        savedState.putString("unit_title", mCurrentTitle);
        savedState.putParcelableArrayList("unit_data", mUnitList);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("onSaveInstanceState", "true");
        // put the current reference value into saved state so when activity comes back
        // all of the units have the same value as they do now
        //outState.putDouble("reference",mReferenceValue.doubleValue());
        //outState.putBoolean("reference_empty",mRefValueEmpty);
        createSavedState(outState);
    }

    /**
     * Handler is called when user presses 'back' to end this activity.
     *
     * Need to get current values, reference and is the reference set, so that
     * they can be bundled off and sent back to the caller for storage.
     */
    @Override
    public void onBackPressed() {
        Log.d("onBackPressed", "true");
        Intent intent = new Intent();
        intent.putExtra("reference", mReferenceValue.doubleValue());
        intent.putExtra("reference_empty", mRefValueEmpty);
        setResult(Activity.RESULT_OK, intent);
        finish();
        super.onBackPressed();
    }

    private int SENSOR_REQUEST_CODE = 1;

    /**
     * This is called when the unit section activity is finished.
     *
     * When the unit activity finishes it will return back the current reference
     * value and if there is a value set so that it can be passed back the next
     * time that unit activity is launched.
     *
     * @param requestCode   code passed to Activity when it is launched
     * @param resultCode    result of the activity (expect RESULT_OK)
     * @param data          contains data bundle with "reference" and "reference_empty" used
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SENSOR_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                //Log.d("Sensor","onActivityResult");
                Bundle b = data.getExtras();
                if (b != null) {
                    // get reference value from UnitActivity we just came back from
                    // and save it off so it can be restored next time user goes there
                    mReferenceValue = new BigDecimal(b.getDouble("value"));
                    //Log.d("onActivityresult","value = " + mReferenceValue.toString());
                    mRefValueEmpty = false;
                    mRestoreState = false;
                }
            }
        }
    }


    public void onClickSnack(View view) {
        Snackbar snackbar = Snackbar
                .make(view,"Reference unit: "+mRefUnitName, Snackbar.LENGTH_LONG) /*
                .setAction("ACTION", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Snackbar snackbar1 = Snackbar.make(view, "additional message", Snackbar.LENGTH_SHORT);
                        snackbar1.show();
                    }
                }) */ ;

        snackbar.show();
    }


    /**
     * Create the main display with the units of measurement using a custom adapter.
     *
     * @param unitList   a list of the Units to display (e.g. foot, inch, meter)
     */
    private void displayListView( ArrayList<Unit> unitList ) {
        //create an ArrayAdapter from the Array of Unit objects
        dataAdapter = new MyCustomAdapter(this, R.layout.unit_row, unitList);
        ListView listView = (ListView) findViewById(R.id.listView1);
        // Assign adapter to ListView
        listView.setAdapter(dataAdapter);
    }


    /**
     *  Implement a custom adapter for the ListView layout.
     *
     *  Custom adapter to display list of units and conversion values.
     *  It allows user to change value of one unit which will then go and
     *  update the rest of them.
     *  The base layout is R.layout.unit_row
     *  @see UnitActivity
     */
    public class MyCustomAdapter extends ArrayAdapter<Unit> {

        // use typical ViewHolder class for ListView to keep inflated widgets around
        class ViewHolder {
            public TextView title;  //*< title of the unit (e.g. Foot)
            public EditText value;  //*< current value of the unit (e.g. 1.5439)
            public ImageView info;  //*< info button to get dialog with long description of unit
            public ImageView wiki;  //*< wiki button to get wikipedia info
        }

        // constructor must have array of Unit objects to fill out ListView
        public MyCustomAdapter(Context context, int textViewResourceId,
                               ArrayList<Unit> unitList) {
            super(context, textViewResourceId, unitList);
            mUnitList = new ArrayList<>();
            mUnitList.addAll(unitList);
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View view;
            Unit unit = mUnitList.get(position);

            if (convertView == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = vi.inflate(R.layout.unit_row, parent, false);
                final ViewHolder viewHolder = new ViewHolder();
                viewHolder.title = (TextView)view.findViewById(R.id.unitTitle);
                viewHolder.value = (EditText)view.findViewById(R.id.value);
                viewHolder.info  = (ImageView)view.findViewById(R.id.infoButton);
                viewHolder.wiki = (ImageView)view.findViewById(R.id.wikiInfoButton);

                // attach a listener so when user is done entering a number we can update values
                viewHolder.value.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        // new number entered so update our reference value
                        EditText editText = (EditText)v;
                        Unit editedUnit = (Unit)editText.getTag();
                        updRefValue(editedUnit, editText.getText().toString());
                    }
                });
                // attach a listener for the 'info' button
                viewHolder.info.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        ImageView iButton = (ImageView) v;
                        Unit infoUnit = (Unit) iButton.getTag();
                        createInfoDialog(infoUnit.getTitle(), infoUnit.getDescription());
                    }
                });
                // attach a listener for the 'wiki' button
                viewHolder.wiki.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        ImageView iButton = (ImageView) v;
                        Unit infoUnit = (Unit) iButton.getTag();
                        getWikiContent(infoUnit.getWiki());
                    }
                });

                view.setTag(viewHolder);
                viewHolder.value.setTag(unit);
                viewHolder.info.setTag(unit);
                viewHolder.wiki.setTag(unit);
            } else {
                view = convertView;
                ViewHolder holder = (ViewHolder)view.getTag();
                // clickable items need a link to the unit
                holder.value.setTag(unit);
                holder.info.setTag(unit);
                holder.wiki.setTag(unit);
            }

            // unique actions for every view go here
            // grab data needed from "unit" and update the View widgets
            ViewHolder holder = (ViewHolder) view.getTag();
            if ( holder != null) {
                holder.title.setText(unit.getTitle());
                if ( mRefValueEmpty ) {
                    holder.value.setText("");
                } else {
                    holder.value.setText(unit.calculateValue(mReferenceValue));
                }
                // if long description not there, remove the little (i) button
                if ( unit.emptyDescription() ) {
                    holder.info.setVisibility(View.INVISIBLE);
                } else {
                    holder.info.setVisibility(View.VISIBLE);
                }
                if ( unit.emptyWiki() ) {
                    holder.wiki.setVisibility(View.INVISIBLE);
                } else {
                    holder.wiki.setVisibility(View.VISIBLE);
                }
            }

            return view;
        }
    }

    /**
     *  Create the dialog that appears when the little (i) button is clicked.
     *
     *  The information button provides additional information about the unit
     *  of measurement, such as "there are 100 centimeters in a meter".
     *
     *  @param title     the Unit title string for the dialog box (e.g. Kilogram)
     *  @param message   the long description string of the unit of measurement
     */
    public void createInfoDialog(String title, String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle(title);
        builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button - just close it
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    /**
     * Parse the input string of data that makes up a Unit, into an array of string tokens.
     *
     * Each unit is defined in string resources and is made up of one or
     * more tokens separated by the "|" character.  This function parses
     * that line into an array of strings.  Strings are trimmed of white
     * space at the beginning and end with the trim() function.
     *
     * The line may just be one token if it is starting a new unit section
     * such as "Length" or "Mass".  Following it will be lines with 3 or 4
     * tokens:  "title|[information]|scale[|offset]
     *  token 1: the title of the unit such as "Foot" or "Kilogram"
     *  token 2: a long description of the unit.  can be a single space for no information.
     *           note: must be a single space as "||" not parsed correctly by tokenizer
     *  token 3: scale factor, how many of this unit are in a reference unit
     *  token 4: optional, added during the conversion such as the Celsius to Fahrenheit
     *  token 5: optional, the Wikipedia web link for content about this unit
     *
     * @param input   the resource string, such as "Inch|an inch is...|3.9370080000E+01"
     * @return        the input string broken up into tokens.  e.g. {[Inch],[an inch is...],[3.9370080000E+01]}
     */
    static public ArrayList<String> parseStringLine( String input ) {
        ArrayList<String> out = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(input,"|");
        while (tokenizer.hasMoreTokens()) {
            out.add(tokenizer.nextToken().trim());
        }
        return out;
    }



    private void getWikiContent(String theUnit)
    {
        mSavedBundle.clear();
        createSavedState(mSavedBundle);
        mRestoreState = true;
        //Log.d("getWikiContent", "bundle: " + mSavedBundle.toString());

        Request request = new Request.Builder()
                .url("https://en.wikipedia.org/w/api.php?action=query&prop=extracts&format=json&exintro=&titles="+theUnit)
                .build();

        Call call = mOkHttpClient.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.d("okhttp", "failed!");
                Intent intent = new Intent(getApplicationContext(), WikiActivity.class);
                intent.putExtra("html", "No Wikipedia data available (offline?)");
                startActivity(intent);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (!response.isSuccessful())
                    throw new IOException("Unexpected code " + response);
                //Log.d("okhttp", "good response!");
/*
                Headers responseHeaders = response.headers();

                for (int i = 0; i < responseHeaders.size(); i++) {
                    Log.d("okhttp", responseHeaders.name(i) + ": " + responseHeaders.value(i));
                }
*/
                String body = response.body().string();
                //Log.d("okhttp", body);

                String data = "No Wikipedia data available";
                try {
                    JSONObject json = new JSONObject(body);
                    JSONObject query = json.getJSONObject("query");
                    JSONObject pages = query.getJSONObject("pages");
                    // below {"query":{"pages":{ is another object with a page ID.  We don't know
                    // what this is so simply get an iterator to get the first child and use that
                    int nkey = pages.length();
                    //Log.d("wiki","length="+nkey);
                    if (nkey > 0) {
                        Iterator<String> keyit = pages.keys();
                        String mainKey = keyit.next();
                        // now we have key below "pages" we can get this child object
                        JSONObject collection = pages.getJSONObject(mainKey);
                        // below that there are several pairs of data and we want "extract":"..."
                        data = collection.getString("extract");
                    }
                } catch (JSONException e) {
                    Log.d("JSON", "error");
                }

                Intent intent = new Intent(getApplicationContext(), WikiActivity.class);
                intent.putExtra("html", data);
                startActivity(intent);
            }
        });
    }

}
