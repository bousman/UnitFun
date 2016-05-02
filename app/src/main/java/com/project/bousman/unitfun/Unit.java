package com.project.bousman.unitfun;


import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * @file
 * @brief This file implements the Unit class, containing information about a particular unit item.
 */


/**
 * The Unit class defines one unit item such as "Foot" or "Pound".
 *
 * Each unit item has a title, long description, and the factors needed to
 * get from the reference value to this unit.
 *
 * The function calculateValue() is used to get from the reference value
 * to a value of this unit and the function calculateReference() does the
 * opposite - calculate the reference value given this unit's value.
 * For example, the unit section "Length" has the reference value defined
 * on the Meter.  If this Unit was "Centimeter" then the scale factor would
 * be 100 and the offset 0 as it takes multiplying 100 by Meter to get
 * Centimeter.
 * If the reference value is 2 then calculateValue(2.0) would return "200.0"
 * which could be put in the EditText box for Centimeter.
 * The opposite is needed when the user types into the EditText.  If they
 * were to type in 150.0 then calculateReference(150.0) which would return
 * 1.5 Meters.  Now all the other units for Length could be calculated from
 * the 1.5 reference.
 */
public class Unit implements Parcelable {
    private int id = 0;
    // title of the unit, such as "Kilometer" or "Foot"
    private String title = "error";
    private String description = "";
    // conversion factor is value needed to multiply by reference unit to get
    // this unit.  For example, if the Length reference unit is the Meter and
    // this unit is Kilometer then the scale is 0.001.
    private BigDecimal convertFromRefScale = new BigDecimal("1.0");
    // sometimes an offset value is needed in addition to a scale when converting
    // units, as with temperature.  Normally it is zero.
    private BigDecimal offset = new BigDecimal("0.0");
    private String wikiTitle = "";


    @SuppressWarnings("unused")
    public Unit(int id, String title, String description, String scale, String offset, String wiki) {
        super();
        this.id = id;
        setTitle(title);
        setDescription(description);
        setWiki(wiki);

        try {
            this.convertFromRefScale = new BigDecimal(scale);
            this.offset = new BigDecimal(offset);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a Unit from the token strings {title,description,scale,offset}
     * Normally a Unit has the title, description, and scale factor.  The offset usually is
     * not needed for most conversions.
     * The ArrayList can be from 0 to 5 in size.  0 means no values which results in default
     * values of a Unit with "error" title.
     *
     * @param id       id number to use for this unit
     * @param tokens   array of optional strings that make up {title,description,scale,offset}
     */
    public Unit( int id, ArrayList<String> tokens ) {
        super();
        this.id = id;
        int tsize = tokens.size();
        if ( tsize >= 1 ) {
            setTitle(tokens.get(0));
        }
        if ( tsize >= 2 ) {
            setDescription(tokens.get(1));
        }
        if ( tsize >= 3 ) {
            setScale(tokens.get(2));
        }
        if ( tsize >= 4 ) {
            setOffset(tokens.get(3));
        }
        if ( tsize >= 5 ) {
            setWiki(tokens.get(4));
        }
    }

    @Override
    public String toString() {
        return "{ ID : " + String.valueOf(this.id)
                + "; Title : " + title
                + "; Scale : " + convertFromRefScale.toString()
                + "; Offset : " + offset.toString()
                + "; Wiki : " + wikiTitle
                + " }";
    }

    @SuppressWarnings("unused")
    public String toJson() {
        try {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("title", title);
            jsonObj.put("description", description);
            jsonObj.put("scale",convertFromRefScale.doubleValue());
            jsonObj.put("offset",offset.doubleValue());
            jsonObj.put("wiki", wikiTitle);

            return jsonObj.toString();
        }
        catch(JSONException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    protected Unit(Parcel in) {
        id = in.readInt();
        title = in.readString();
        description = in.readString();
        convertFromRefScale = (BigDecimal) in.readValue(BigDecimal.class.getClassLoader());
        offset = (BigDecimal) in.readValue(BigDecimal.class.getClassLoader());
        wikiTitle = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeValue(convertFromRefScale);
        dest.writeValue(offset);
        dest.writeString(wikiTitle);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Unit> CREATOR = new Parcelable.Creator<Unit>() {
        @Override
        public Unit createFromParcel(Parcel in) {
            return new Unit(in);
        }

        @Override
        public Unit[] newArray(int size) {
            return new Unit[size];
        }
    };


    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    //* is the description text empty?
    public boolean emptyDescription() {
        return ("".equals(this.description));
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String desc) { this.description = desc; }

    public void setScale(String scale) {
        this.convertFromRefScale = new BigDecimal(scale);
    }
    public BigDecimal getScale() { return this.convertFromRefScale; }

    public void setOffset(String offset) {
        if (offset != null) {
            try {
                this.offset = new BigDecimal(offset);
            } catch (Exception e) {
            }
        }
    }

    public void setWiki(String wiki) { this.wikiTitle = wiki; }
    public String getWiki() { return wikiTitle; }
    public boolean emptyWiki() {
        return ("".equals(this.wikiTitle));
    }

    /**
     * Calculate how many of these units we have given the master reference value
     * and return it from a string.  For the distance units if the master is tied
     * to "meters" then if we have 1 meter then multiply by the scale factor to get
     * how many centimeters, feet, inches, etc.
     *
     * @param fromMaster    the current master reference value
     * @return              the current unit value as a string, calculated from the master reference
     */
    //
    public String calculateValue(BigDecimal fromMaster) {
        // unit value = reference * scale + offset
        BigDecimal v = fromMaster.multiply(convertFromRefScale);
        v = v.add(offset);
        return Float.toString(v.floatValue());
    }


    /**
     * The opposite of calculateValue, this is returning how many units of the reference
     * value there are if we have 'fromValue' units of this unit.  So if we have 100
     * of this unit which is centimeters it would say there is 1 of the reference unit
     * since the scale factor is tied to meters for distance.
     *
     * @param fromValue     the current value of this unit of measurement
     * @return              the master reference calculated from this unit
     */
    public BigDecimal calculateReference(BigDecimal fromValue) {
        try {
            BigDecimal t = fromValue.subtract(this.offset);
            if (this.convertFromRefScale.doubleValue() != 0.0)
                return t.divide(this.convertFromRefScale, 16, BigDecimal.ROUND_HALF_DOWN);
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return new BigDecimal(0.0);
    }
}
