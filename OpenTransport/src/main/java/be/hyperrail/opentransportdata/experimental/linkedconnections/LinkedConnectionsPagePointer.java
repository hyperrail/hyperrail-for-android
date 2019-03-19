package be.hyperrail.opentransportdata.experimental.linkedconnections;

import be.hyperrail.opentransportdata.common.contracts.NextDataPointer;

public class LinkedConnectionsPagePointer implements NextDataPointer {
    private String value;

    LinkedConnectionsPagePointer(String value) {
        this.value = value;
    }

    @Override
    public String getPointer() {
        return value;
    }

}
