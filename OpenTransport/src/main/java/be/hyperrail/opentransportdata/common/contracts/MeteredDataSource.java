/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.contracts;

import android.net.TrafficStats;
import android.support.annotation.IntDef;

/**
 * A metered API allows to get details on network usage
 */
public interface MeteredDataSource {

    int RESPONSE_ONLINE = 1;
    int RESPONSE_CACHED = 2;
    int RESPONSE_OFFLINE = 4;
    int RESPONSE_FAILED = 8;

    MeteredRequest[] getMeteredRequests();
    @IntDef({RESPONSE_ONLINE, RESPONSE_CACHED, RESPONSE_OFFLINE, RESPONSE_FAILED})
    @interface responseType {
    }

    class MeteredRequest {
        private String mTag;
        private long mBytesSent = 0;
        private long mBytesReceived = 0;
        private long mMsecStart = 0;
        private long mMsecUsableResult = 0;
        private long mMsecParsed = 0;
        private int mResponseType;
        private long rxBbytesAtStart = 0;
        private long txBbytesAtStart = 0;

        public String getTag() {
            return mTag;
        }

        public void setTag(String tag) {
            mTag = tag;
        }

        public long getBytesSent() {
            return mBytesSent;
        }

        public long getBytesReceived() {
            return mBytesReceived;
        }

        public long getMsecStart() {
            return mMsecStart;
        }

        public void setMsecStart(long msecStart) {
            mMsecStart = msecStart;
            rxBbytesAtStart = TrafficStats.getTotalRxBytes();
            txBbytesAtStart = TrafficStats.getTotalTxBytes();
        }

        public long getMsecUsableResult() {
            return mMsecUsableResult;
        }

        public long getMsecParsed() {
            return mMsecParsed;
        }

        public void setMsecParsed(long msecParsed) {
            if (mMsecParsed == 0) {
                mMsecParsed = msecParsed;
            }
            mBytesReceived = TrafficStats.getTotalRxBytes() - rxBbytesAtStart;
            mBytesSent = TrafficStats.getTotalTxBytes() - txBbytesAtStart;
        }

        public void setMsecUsableNetworkResponse(long msecFirstByte) {
            if (mMsecUsableResult == 0) {
                mMsecUsableResult = msecFirstByte;
            }
        }

        public int getResponseType() {
            return mResponseType;
        }

        public void setResponseType(@responseType int responseType) {
            mResponseType |= responseType;
        }

        String getResponseTypeList() {
            String result = "";
            if ((mResponseType & 1) == 1) {
                result += "Online;";
            }
            if ((mResponseType & 2) == 2) {
                result += "Cached;";
            }
            if ((mResponseType & 4) == 4) {
                result += "Offline;";
            }
            if ((mResponseType & 8) == 8) {
                result += "Failed";
            }
            if (result.endsWith(";")) {
                result = result.substring(0, result.length() - 1);
            }
            return result;
        }

        @Override
        public String toString() {
            return mTag + "," + mMsecStart + "," + mMsecUsableResult + "," + mMsecParsed + "," + mBytesReceived + "," + mBytesSent + "," + getResponseTypeList();
        }
    }
}
