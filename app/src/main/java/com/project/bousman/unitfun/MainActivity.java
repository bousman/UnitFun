package com.project.bousman.unitfun;

/**
 * @file
 * @brief This file implements the main activity of the UnitFun app
 *
 * This program performs conversion between units of measurement.  The main
 * screen is a list of unit classes, such as "Length" or "Mass".  For each
 * class there is a list of units such as "Foot", "Inch", "Meter".  Enter
 * a value in one unit and it will show up in all the other units but converted
 * to the proper value.  e.g. enter "12" into "Inch" and you will see "1" for
 * foot, etc.
 *
 * All of the units data comes from the resource string-array "data".
 *
 * @author Brian Bousman
 * @version 0.7.0
 *
 * @see strings.xml
 * @see UnitActivity
 * @see Unit
 */

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;


/**
 * The main activity has to read in all of the conversion unit data from the string array
 * resource and parse it.  It constructs a ListView using the conversion sections (e.g.
 * "Length", "Area", "Mass").  Each item in the list is the name of that section and acts
 * as a button to launch the page for that section.
 */
public class MainActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener {

    /**
     * Class UnitActivityData holds data for each UnitActivity page.
     *
     * Each Unit section (e.g. Length, Mass, Speed) has different data that needs
     * to be saved, such as the units of measurement for that unit, the current
     * reference value, and if that page has been called at all.
     * The internal arrays MUST be the same size since they are sized for the
     * number of unit sections.
     */
    private class UnitActivityData {
        //* title of each unit section (e.g. Length,Mass)
        private ArrayList<String> dataTitles = new ArrayList<>();
        /**
         * dataBundles contains the title and all the units of measurement
         * for each section.  The keys will be "unit_title" and "unit_data".
         * "unit_title" key will get the section title (same as dataTitles)
         * "unit_data" will get an ArrayList<Unit> object, each element of
         *             this array will be for one unit of measurement containing
         *             measurement title, description, and conversion factors.
         *             Unit is a parcelable object so it can be added to bundle directly
         */
        private ArrayList<Bundle> dataBundles = new ArrayList<>();
        //* the current reference value for that unit section
        private ArrayList<Double> dataReference = new ArrayList<>();
        //* is there a reference value set for this section?  false=leave boxes blank
        private ArrayList<Boolean> dataHasRefSet = new ArrayList<>();

        public UnitActivityData() {}

        /**
         * Add a Unit Section to the data.  Only done once for each section!
         *
         * @param title     the title of that section (e.g. "Length")
         * @param b         a bundle of data for that section (title, description, conversion)
         *                  @see UnitActivityData#dataBundles
         */
        public void add( String title, Bundle b ) {
            dataTitles.add(title);
            dataBundles.add(b);
            dataReference.add(0.);
            dataHasRefSet.add(false);
        }

        public ArrayList<String> titles() { return dataTitles; }
        public Bundle getBundle(int index) { return dataBundles.get(index); }
        public Double getReference(int index) { return dataReference.get(index); }
        public void setReference(int index, Double newRef) { dataReference.set(index,newRef); }
        public Boolean getHasRefSet(int index) { return dataHasRefSet.get(index); }
        public void setHasRefSet(int index, Boolean newRef) { dataHasRefSet.set(index,newRef); }
    }

    // this holds data about each unit section to pass to the UnitActivity when launched
    private UnitActivityData mUnitActData = new UnitActivityData();

    // integer has "position" from ListView that was clicked on to launch activity
    // for new unit page.  Effectively the position in the dataBundles and dataTitles
    // arrays when UnitActivity was launched.  This is used in onActivityResult
    // to know which page it just returned from
    private int mLastPosition = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        setContentView(R.layout.activity_main);

        // up/back in the toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // load in the unit data from resource strings
        loadDataFromStrings();

        // get location of <ListView> in main layout and fill it
        // with titles of all the unit sections.
        ListView myListView = (ListView)findViewById(R.id.main_list);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, mUnitActData.titles());
        myListView.setAdapter(adapter);
        myListView.setOnItemClickListener(mMessageClickedHandler);
    }

    /**
     *  Load data from the string resource and pack it up.
     *
     *  The string resources have all of the unit names and conversion
     *  data.  This reads it, parses it, and packs it into member variables
     *  so it can be used each time a new activity is launched.
     *  The data is put into "dataTitles" and "dataBundles".
     */
    private void loadDataFromStrings() {
        Resources res = getResources();
        String[] conversionData = res.getStringArray(R.array.data);
        Bundle b = new Bundle();   // will hold title and all units
        String title = "";
        ArrayList<Unit> unitLines = new ArrayList<>();  // builds lines of conversion data
        for ( int ii = 0; ii < conversionData.length; ++ii ) {
            ArrayList<String> parsed = UnitActivity.parseStringLine(conversionData[ii]);
            // if only one item in the line it means a new set of units
            if ( parsed.size() == 1 ) {
                if ( ii > 0 ) {
                    // save off units for this section into the bundle (it's an array of parcelable objects)
                    b.putParcelableArrayList("unit_data", unitLines);
                    mUnitActData.add(title,b);
                    b = new Bundle();
                    unitLines = new ArrayList<>();
                }
                title = parsed.get(0);
                b.putString("unit_title",title);
            } else {
                // convert string into a "unit" object and add to array list
                try {
                    Unit unit = new Unit(ii, parsed);
                    unitLines.add(unit);
                }  catch (Exception e) {
                    Log.d("data problem", "***************************************************");
                    Log.d("data problem", conversionData[ii]);
                    Log.d("data problem", "***************************************************");
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Error in data input strings - see log!")
                            .setTitle("Data Error");
                    builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User clicked OK button - just close it
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        }
        // at end of data array there is one more set of data to add to bundle
        b.putParcelableArrayList("unit_data", unitLines);
        mUnitActData.add(title, b);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Toast.makeText(this, "onCreateOptionsMenu", Toast.LENGTH_LONG).show();

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_settings:
                launchSettings();
                return true;
            case R.id.action_about:
                createAboutDialog(getString(R.string.app_name),
                        getString(R.string.app_author));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            Toast.makeText(this, "onMenuItemClick", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    /**
     *  This is the click listener for the main ListView, to launch new pages
     *
     *  The main activity is a ListView built using each of the unit sections
     *  (e.g. Length, Mass, etc.).  Clicking on one of those list elements
     *  activates this listener which then launches a new activity for that
     *  unit section.
     */
    private AdapterView.OnItemClickListener mMessageClickedHandler = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            Intent intent = new Intent(v.getContext(), UnitActivity.class);
            // resource strings containing unit data was preloaded into mUnitActData during
            // the loadDataFromStrings() function call.  It is already a Bundle
            intent.putExtras(mUnitActData.getBundle(position));
            intent.putExtra("reference",mUnitActData.getReference(position));
            intent.putExtra("reference_set",mUnitActData.getHasRefSet(position));
            mLastPosition = position;  // save for onActivityResult
            startActivityForResult(intent, MAIN_REQUEST_CODE);
        }
    };

    private int MAIN_REQUEST_CODE = 1;

    /**
     * This is called when the unit section activity is finished.
     *
     * When the unit activity finishes it will return back the current reference
     * value and if there is a value set so that it can be passed back the next
     * time that unit activity is launched.
     *
     * @param requestCode   code passed to Activity when it is launched
     * @param resultCode    result of the activity (expect RESULT_OK)
     * @param data          contains data bundle with "reference" and "reference_set" used
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MAIN_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Bundle b = data.getExtras();
                if (b != null) {
                    // get reference value from UnitActivity we just came back from
                    // and save it off so it can be restored next time user goes there
                    double ref = b.getDouble("reference");
                    Boolean refSet = b.getBoolean("reference_set");
                    saveReferenceValue(mLastPosition,ref,refSet);
                }
            }
        }
    }

    /**
     *  Create an "About" dialog to show user info about this program.
     *
     *  Creates a normal dialog box using the AlertDialog, fills it
     *  with the title and message and a single Ok button.  No cancel.
     *
     *  @param title     the title of the dialog box (program name)
     *  @param message   the message to go in the dialog box
     */
    public void createAboutDialog(String title, String message)
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
     * Launch the "settings" activity
     */
    private void launchSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Save off the reference value at end of each UnitActivity call.
     *
     * The reference value, and if there was one set, is all that is needed when
     * launching that activity the next time to have the value boxes filled.  In other
     * words if the Length activity returns 1.0 and true it means we can pass that
     * back the next time Length is launched and it will have the reference 1.0 for Meter
     * and all the unit of measurements can be calculated from that, restoring it to
     * the same state as when it ended.
     *
     * @param position          index to unit activity (0 to n)
     * @param referenceValue    reference value for that unit activity
     * @param referenceSet      is there a reference value set?
     */
    private void saveReferenceValue( int position, double referenceValue, Boolean referenceSet ) {
        mUnitActData.setReference(position, referenceValue);
        mUnitActData.setHasRefSet(position, referenceSet);
    }
}
