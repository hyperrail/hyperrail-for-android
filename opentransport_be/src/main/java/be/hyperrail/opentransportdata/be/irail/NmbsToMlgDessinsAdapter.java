/*
 *  This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file,
 *  You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.be.irail;

public class NmbsToMlgDessinsAdapter {

    private NmbsToMlgDessinsAdapter() {

    }

    static NmbsTrainType convert(String parentType, String subType, String orientation, int firstClassSeats) {
        if (parentType.startsWith("HLE")) {
            return convertHle(parentType, subType, orientation);
        } else if (parentType.startsWith("AM") || parentType.startsWith("MR") || parentType.startsWith("AR")) {
            return convertAm(parentType, subType, orientation, firstClassSeats);
        } else if (parentType.startsWith("I") || parentType.startsWith("M")) {
            return convertCarriage(parentType, subType, orientation, firstClassSeats);
        } else {
            return new NmbsTrainType(parentType, subType, orientation);
        }
    }

    private static NmbsTrainType convertCarriage(String parentType, String subType, String orientation, int firstClassSeats) {
        String newParentType = parentType;
        String newSubType = subType;
        switch (parentType) {
            case "M4":
                switch (subType) {
                    case "A":
                    case "AU":
                        newSubType = "B_A";
                        break;
                    case "AD":
                    case "AUD":
                        newSubType = "B_AD";
                        break;
                    case "ADX":
                        newSubType = "B_ADX";
                        break;
                    case "B":
                    case "BU":
                    case "BYU":
                        newSubType = "B_B";
                        break;
                    case "BD":
                    case "BDU":
                        newSubType = "B_BD";
                        break;
                }
                break;
            case "M6":
                switch (subType) {
                    case "BXAA":
                    case "BXCT":
                        // 134/117 2nd class, LIKELY steering cabin
                        newSubType = "BDX";
                        break;
                    case "BYU":
                        // BU + Y
                        break;
                    case "BUH":
                        // BU + H
                    case "BDUH":
                        // BUH + D
                    case "BAU":
                        // Mixed 1st/2nd class
                    case "BU":
                        // 140/133 2nd class
                        newSubType = "B";
                        break;
                    case "AU":
                        // 124/133 1st class
                        newSubType = "A";
                        break;
                    case "BDYU":
                    case "BDU":
                        // 102/145 2nd class w/ luggage and bike storage
                        newSubType = "BD";
                        break;
                    case "BDAU":
                        // 1st/2nd class w/ luggage and bike storage
                        newSubType = "ABD";
                        break;
                }
                break;
            case "M7":
                // Fallback on M6 icons
                switch (subType) {
                    case "BUH":
                        newSubType = "BD";
                        break;
                    case "ABUH":
                        newSubType = "ABD";
                        break;
                }
                break;
            case "I10":
                if (firstClassSeats > 0) {
                    newSubType = "B_A";
                } else {
                    newSubType = "B_B";
                }
                break;
            case "I11":
                if (subType.contains("X")) {
                    newSubType = "BDX";
                } else if (firstClassSeats > 0) {
                    newSubType = "A";
                } else {
                    newSubType = "B";
                }
                break;
        }
        return new NmbsTrainType(newParentType, newSubType, orientation);
    }

    private static NmbsTrainType convertAm(String parentType, String subType, String orientation, int firstClassSeats) {
        String newParentType = parentType;
        String newSubType = subType;

        switch (parentType) {
            case "AM08":
            case "AM08M":
                switch (subType) {
                    case "A":
                    case "C":
                        newSubType = "0_C";
                        break;
                    case "B":
                        newSubType = "0_B";
                        break;
                }
                newParentType = "AM08";
                break;
            case "AM08P":
                newParentType = "AM08";
                switch (subType) {
                    case "A":
                    case "C":
                        newSubType = "5_C";
                        break;
                    case "B":
                        newSubType = "5_B";
                        break;
                }
                break;
            case "AM86":
                if (firstClassSeats > 0) {
                    newSubType = "R_B";
                } else {
                    newSubType = "M_B";
                }
                break;
            case "AR41":
                newParentType = "MW41";
                if (firstClassSeats > 0) {
                    newSubType = "AB";
                } else {
                    newSubType = "B";
                }
                break;
            case "AM75":
                switch (subType) {
                    case "A":
                    case "D":
                        if (firstClassSeats > 0) {
                            newSubType = "RXA_B";
                        } else {
                            newSubType = "RXB_B";
                        }
                        break;
                    case "B":
                        newSubType = "M1_B";
                        break;
                    case "C":
                        newSubType = "M2_B";
                        break;
                }
                break;
            case "AM80":
            case "AM80M":
                newParentType = "AM80";
                switch (subType) {
                    // B, BX, ABDX,
                    case "A":
                    case "C":
                        if (firstClassSeats > 0) {
                            newSubType = "ABDX_B";
                        } else {
                            newSubType = "BX_B";
                        }
                        break;
                    case "B":
                        newSubType = "B_B";
                        break;
                }
                break;
            case "AM62-66":
                newParentType = "AM66";
                if (firstClassSeats > 0) {
                    newSubType = "M2_B";
                } else {
                    newSubType = "M1_B";
                }
                break;
            case "AM96":
            case "AM96M":
            case "AM96P":
                newParentType = "AM96";
                switch (subType) {
                    // B, BX, ABDX,
                    case "A":
                    case "C":
                        if (firstClassSeats > 0) {
                            newSubType = "AX";
                        } else {
                            newSubType = "BX";
                        }
                        break;
                    case "B":
                        newSubType = "BBIC";
                        break;
                }
                break;
        }
        return new NmbsTrainType(newParentType, newSubType, orientation);
    }

    private static NmbsTrainType convertHle(String parentType, String subType, String orientation) {
        String newParentType = parentType;
        String newSubType = subType;

        switch (parentType) {
            case "HLE18":
                // NMBS doesn't distinguish between the old and new gen. All the old gen vehicles are out of service.
                newParentType += "II";
                newSubType = "";
                break;
            case "HLE11":
            case "HLE12":
            case "HLE13":
            case "HLE15":
            case "HLE16":
            case "HLE19":
            case "HLE20":
            case "HLE21":
                if (subType.isEmpty()) {
                    newSubType = "B";
                }
                break;
        }

        return new NmbsTrainType(newParentType, newSubType, orientation);
    }
}