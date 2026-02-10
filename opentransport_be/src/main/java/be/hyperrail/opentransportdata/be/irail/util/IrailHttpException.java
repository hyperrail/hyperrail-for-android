package be.hyperrail.opentransportdata.be.irail.util;

import org.json.JSONObject;

import java.io.IOException;

public class IrailHttpException extends IOException {
    private final int code;
    private final JSONObject response;

    public IrailHttpException(int code, JSONObject response) {
        this.code = code;
        this.response = response;
    }

    public int getCode() {
        return code;
    }

    public JSONObject getResponse() {
        return response;
    }
}
