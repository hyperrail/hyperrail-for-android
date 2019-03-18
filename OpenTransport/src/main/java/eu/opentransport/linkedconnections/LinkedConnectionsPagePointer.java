package eu.opentransport.linkedconnections;

import eu.opentransport.common.contracts.NextDataPointer;

public class LinkedConnectionsPagePointer implements NextDataPointer {
    private String value;

    public LinkedConnectionsPagePointer(String value) {
        this.value = value;
    }

    @Override
    public String getPointer() {
        return value;
    }

}
