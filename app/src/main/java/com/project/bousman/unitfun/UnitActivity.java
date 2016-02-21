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
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.StringTokenizer;


/**
 * The UnitActivity class handles displaying a list of units (e.g foot, inch).
 * It uses a ListView with custom adapter to show the unit name, current value,
 * and an informational button to get a longer description of the unit.
 */
public class UnitActivity extends AppCompatActivity {

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

        // reference value from last time at this particular UnitActivity is passed in
        // (which will be zero on first time or if EditText boxes are empty)
        Bundle b = getIntent().getExtras();

        // the reference value is part of the saved state so when the screen
        // rotates we won't lose our values
        if ( savedInstanceState != null ) {
            try {
                double d = savedInstanceState.getDouble("reference");
                mReferenceValue = new BigDecimal(d);
                mRefValueEmpty = savedInstanceState.getBoolean("reference_empty");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // if the "keep values" setting is turned on then use the reference value
            // from the previous call to this activity that is being passed back through
            // the Bundle
            if ( keep_values ) {
                mReferenceValue = new BigDecimal(b.getDouble("reference"));
                mRefValueEmpty = !b.getBoolean("reference_set");
            } /*else {
                // This is first time into this activity or the "keep values" setting is off
                // so use default values
            } */
        }

        String title = b.getString("unit_title");   // page title

        // up/back in the toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        if (Build.VERSION.SDK_INT > 19) {
            setSupportActionBar(myToolbar);
            ActionBar bar = getSupportActionBar();
            if (bar != null) {
                bar.setTitle(title);
                bar.setDisplayHomeAsUpEnabled(true);
            }
        }
        else {
            myToolbar.setVisibility(View.GONE);
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
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // put the current reference value into saved state so when activity comes back
        // all of the units have the same value as they do now
        outState.putDouble("reference",mReferenceValue.doubleValue());
        outState.putBoolean("reference_empty",mRefValueEmpty);
    }

    /**
     * Handler is called when user presses 'back' to end this activity.
     *
     * Need to get current values, reference and is the reference set, so that
     * they can be bundled off and sent back to the caller for storage.
     */
    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("reference", mReferenceValue.doubleValue());
        intent.putExtra("reference_set", !mRefValueEmpty);
        setResult(Activity.RESULT_OK, intent);
        finish();
        super.onBackPressed();
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

                view.setTag(viewHolder);
                viewHolder.value.setTag(unit);
                viewHolder.info.setTag(unit);
            } else {
                view = convertView;
                ViewHolder holder = (ViewHolder)view.getTag();
                // clickable items need a link to the unit
                holder.value.setTag(unit);
                holder.info.setTag(unit);
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
                    holder.info.setVisibility(View.GONE);
                } else {
                    holder.info.setVisibility(View.VISIBLE);
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
}
