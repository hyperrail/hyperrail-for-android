package be.hyperrail.opentransportdata.common.models.implementation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import be.hyperrail.opentransportdata.common.models.Message;

class MessageImplTest {

    @Test
    void getMessageDetails_fieldsAreSet_shouldReturnCorrectValues() {
        Message m = new MessageImpl("headerText", "descriptionText", "linkText");
        Assertions.assertEquals("headerText", m.getHeader());
        Assertions.assertEquals("descriptionText", m.getDescription());
        Assertions.assertEquals("linkText", m.getLink());
    }

    @Test
    void getMessageDetails_fieldsAreEmpty_shouldReturnCorrectValues() {
        Message m = new MessageImpl("", "", "");
        Assertions.assertEquals("", m.getHeader());
        Assertions.assertEquals("", m.getDescription());
        Assertions.assertEquals("", m.getLink());
    }

    @Test
    void getMessageDetails_fieldsAreNull_shouldReturnCorrectValues() {
        Message m = new MessageImpl(null, null, null);
        Assertions.assertNull(m.getHeader());
        Assertions.assertNull(m.getDescription());
        Assertions.assertNull(m.getLink());
    }
}