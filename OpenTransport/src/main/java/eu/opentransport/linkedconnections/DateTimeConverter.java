/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.opentransport.linkedconnections;

import com.bluelinelabs.logansquare.typeconverters.TypeConverter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;

public class DateTimeConverter implements TypeConverter<DateTime> {
    private static final DateTimeZone TZ_BRUSSELS = DateTimeZone.forID("Europe/Brussels");
    private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-ddd'T'HH:mm:ss.SSSZ");

    @Override
    public DateTime parse(JsonParser jsonParser) throws IOException {
        String dateString = jsonParser.getValueAsString(null);
        try {
            return formatter.parseDateTime(dateString).withZone(TZ_BRUSSELS);
        } catch (RuntimeException runtimeException) {
            runtimeException.printStackTrace();
            return null;
        }
    }

    @Override
    public void serialize(DateTime object, String fieldName, boolean writeFieldNameForObject, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStringField(fieldName, formatter.print(object));
    }
}
