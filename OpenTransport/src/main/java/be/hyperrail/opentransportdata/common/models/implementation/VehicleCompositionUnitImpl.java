/*
 *  This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file,
 *  You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.models.implementation;

import be.hyperrail.opentransportdata.common.models.VehicleCompositionUnit;

public class VehicleCompositionUnitImpl implements VehicleCompositionUnit {


    private int drawableResource;
    private Integer publicFacingNumber;
    private String publicTypeName;
    private boolean hasToilet;
    private boolean canPassToNextUnit;

    public VehicleCompositionUnitImpl(int drawableResource, Integer publicFacingNumber, String publicTypeName, boolean hasToilet, boolean canPassToNextUnit) {
        this.drawableResource = drawableResource;
        this.publicFacingNumber = publicFacingNumber;
        this.publicTypeName = publicTypeName;
        this.hasToilet = hasToilet;
        this.canPassToNextUnit = canPassToNextUnit;
    }

    @Override
    public int getDrawableResourceId() {
        return drawableResource;
    }

    @Override
    public String getPublicTypeName() {
        return publicTypeName;
    }

    @Override
    public Integer getPublicFacingNumber() {
        return publicFacingNumber;
    }

    @Override
    public boolean hasToilet() {
        return hasToilet;
    }

    @Override
    public boolean canPassToNextUnit() {
        return canPassToNextUnit;
    }
}
