package be.hyperrail.opentransportdata.common.models.implementation;

import be.hyperrail.opentransportdata.common.contracts.NextDataPointer;

public class StringPagePointer implements NextDataPointer {

    String value;

    public StringPagePointer(String value) {
        this.value = value;
    }

    @Override
    public String getPointer() {
        return value;
    }
}
