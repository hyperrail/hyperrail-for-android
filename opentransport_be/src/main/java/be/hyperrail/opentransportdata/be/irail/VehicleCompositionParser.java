/*
 *  This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file,
 *  You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.be.irail;

import android.content.Context;
import android.content.res.Resources;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import be.hyperrail.opentransportdata.common.models.VehicleCompositionUnit;
import be.hyperrail.opentransportdata.common.models.implementation.VehicleCompositionImpl;
import be.hyperrail.opentransportdata.common.models.implementation.VehicleCompositionUnitImpl;
import be.hyperrail.opentransportdata.logging.OpenTransportLog;

class VehicleCompositionParser {

    private static final OpenTransportLog log = OpenTransportLog.getLogger(VehicleCompositionParser.class);

    VehicleCompositionParser() {
    }

    VehicleCompositionImpl parseVehicleComposition(Context appContext, JSONObject response, String vehicleId) throws JSONException {

        JSONArray segmentsArray = response.getJSONObject("composition").getJSONObject("segments").getJSONArray("segment");
        // Get the longest composition to skip trains that are too short (locomotive only, incorrect compositions)
        JSONObject longestSegment = getLongestComposition(segmentsArray);

        JSONArray units = longestSegment.getJSONObject("composition").getJSONObject("units").getJSONArray("unit");
        boolean confirmed = !longestSegment.getJSONObject("composition").getString("source").equalsIgnoreCase("planning");
        VehicleCompositionUnit[] vehicleCompositionUnits = new VehicleCompositionUnit[units.length()];
        for (int i = 0; i < units.length(); i++) {
            vehicleCompositionUnits[i] = parseVehicleCompositionUnit(appContext, units.getJSONObject(i), i);
        }
        return new VehicleCompositionImpl(vehicleCompositionUnits, confirmed);
    }

    private JSONObject getLongestComposition(JSONArray segmentsArray) throws JSONException {
        JSONObject longestSegment = segmentsArray.getJSONObject(0);
        if (segmentsArray.length() < 2){
            return longestSegment;
        }
        int longestSegmentLength = longestSegment.getJSONObject("composition").getJSONObject("units").getInt("number");
        for (int i = 1; i < segmentsArray.length(); i++){
            JSONObject segment = segmentsArray.getJSONObject(i);
            int length = segment.getJSONObject("composition").getJSONObject("units").getInt("number");
            if (length > longestSegmentLength){
                longestSegment = segment;
            }
        }
        return longestSegment;
    }

    private VehicleCompositionUnit parseVehicleCompositionUnit(Context appContext, JSONObject jsonObject, int position) throws JSONException {
        String parentType = jsonObject.getJSONObject("materialType").getString("parent_type").toUpperCase();
        String subType = jsonObject.getJSONObject("materialType").getString("sub_type").toUpperCase();
        String orientation = jsonObject.getJSONObject("materialType").getString("orientation").substring(0, 1).toUpperCase();

        boolean canPassToNextUnit = Objects.equals(jsonObject.getString("canPassToNextUnit"), "1");
        Integer publicFacingNumber = getPublicFacingNumber(jsonObject);
        boolean hasToilet = Objects.equals(jsonObject.getString("hasToilets"), "1");
        boolean hasAirco = Objects.equals(jsonObject.getString("hasAirco"), "1");
        int numberOfFirstClassSeats = jsonObject.getInt("seatsFirstClass");
        int numberOfSecondClassSeats = jsonObject.getInt("seatsSecondClass");

        NmbsTrainType trainType = NmbsToMlgDessinsAdapter.convert(parentType, subType, orientation, numberOfFirstClassSeats, position);
        int resourceId = getResourceIdForTrain(appContext, trainType);
        return new VehicleCompositionUnitImpl(resourceId, publicFacingNumber, trainType.parentType, hasToilet, hasAirco, canPassToNextUnit, numberOfFirstClassSeats, numberOfSecondClassSeats);
    }

    private Integer getPublicFacingNumber(JSONObject jsonObject) throws JSONException {
        String publicFacingNumberString = jsonObject.getString("materialNumber");
        Integer publicFacingNumber;
        if (!publicFacingNumberString.isEmpty() && !publicFacingNumberString.equals("0")) {
            publicFacingNumber = Integer.parseInt(publicFacingNumberString);
        } else {
            publicFacingNumber = null;
        }
        return publicFacingNumber;
    }

    private int getResourceIdForTrain(Context appContext, NmbsTrainType trainType) {
        String resourceName = ("sncb_" + trainType.parentType + "_" + trainType.subType + "_" + trainType.orientation).toLowerCase();
        log.info("Getting vehicle image for " + resourceName);
        Resources resources = appContext.getResources();
        int resourceId = resources.getIdentifier(resourceName, "drawable", appContext.getPackageName());

        if (resourceId == 0) {
            // Locomotives don't have a subtype
            resourceName = ("sncb_" + trainType.parentType + "_" + trainType.orientation).toLowerCase();
            resources = appContext.getResources();
            resourceId = resources.getIdentifier(resourceName, "drawable", appContext.getPackageName());
        }

        if (resourceId == 0) {
            log.warning("Could not find image for vehicle " + resourceName);
        }
        return resourceId;
    }
}