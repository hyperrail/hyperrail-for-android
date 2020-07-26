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
        JSONObject segment = response.getJSONObject("composition").getJSONObject("segments").getJSONArray("segment").getJSONObject(0);
        JSONArray units = segment.getJSONObject("composition").getJSONObject("units").getJSONArray("unit");
        boolean confirmed = !segment.getJSONObject("composition").getString("source").equalsIgnoreCase("planning");
        VehicleCompositionUnit[] vehicleCompositionUnits = new VehicleCompositionUnit[units.length()];
        for (int i = 0; i < units.length(); i++) {
            vehicleCompositionUnits[i] = parseVehicleCompositionUnit(appContext, units.getJSONObject(i));
        }
        return new VehicleCompositionImpl(vehicleCompositionUnits, confirmed);
    }

    private VehicleCompositionUnit parseVehicleCompositionUnit(Context appContext, JSONObject jsonObject) throws JSONException {
        String parentType = jsonObject.getJSONObject("materialType").getString("parent_type").toUpperCase();
        String subType = jsonObject.getJSONObject("materialType").getString("sub_type").toUpperCase();
        String orientation = jsonObject.getJSONObject("materialType").getString("orientation").substring(0, 1).toUpperCase();

        boolean canPassToNextUnit = Objects.equals(jsonObject.getString("canPassToNextUnit"), "1");
        Integer publicFacingNumber = getPublicFacingNumber(jsonObject);
        boolean hasToilet = Objects.equals(jsonObject.getString("hasToilets"), "1");
        boolean hasAirco = Objects.equals(jsonObject.getString("hasAirco"), "1");
        int numberOfFirstClassSeats = jsonObject.getInt("seatsFirstClass");
        int numberOfSecondClassSeats = jsonObject.getInt("seatsSecondClass");

        NmbsTrainType trainType = NmbsToMlgDessinsAdapter.convert(parentType, subType, orientation, numberOfFirstClassSeats);
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