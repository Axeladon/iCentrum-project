package org.example.scraper.service.utils;

public class CrmCodeUtil {
    private CrmCodeUtil() {}

    public static String getMemoryCode(String memory) {
        if (memory == null) return null;

        memory = memory.trim().toUpperCase();

        return switch (memory) {
            case "16"          -> "1";
            case "32"          -> "2";
            case "64"          -> "3";
            case "128"         -> "4";
            case "256"         -> "5";
            case "512"         -> "6";
            case "1024", "1TB" -> "7";
            case "2048", "2TB" -> "8";
            default -> null;
        };
    }

    public static Integer getGradeCode(String grade) {
        return switch (grade) {
            case "A" -> 1;
            case "AB" -> 2;
            case "B" -> 3;
            case "BC" -> 4;
            case "C" -> 5;
            default -> null;
        };
    }

    public static String getColorCode(String color) {
        if (color == null) return null;

        color = color.trim().toLowerCase();

        return switch (color) {
            case "gold" -> "1";
            case "silver" -> "2";
            case "space gray" -> "3";
            case "red" -> "4";
            case "white" -> "5";
            case "black" -> "6";
            case "coral" -> "7";
            case "yellow" -> "8";
            case "blue" -> "9";
            case "green" -> "10";

            case "midnight green" -> "12";
            case "graphite" -> "13";
            case "pacific blue" -> "14";
            case "rose gold" -> "15";
            case "matte black" -> "16";
            case "jet black" -> "17";
            case "purple" -> "18";
            case "starlight" -> "19";
            case "pink" -> "20";
            case "midnight" -> "21";
            case "sierra blue" -> "22";
            case "deep purple" -> "23";
            case "natural titanium" -> "24";
            case "blue titanium" -> "25";
            case "white titanium" -> "26";
            case "black titanium" -> "27";
            case "teal" -> "28";
            case "ultramarine" -> "29";
            case "desert titanium" -> "30";
            case "steel gray" -> "31";
            case "lavender" -> "32";
            case "mist blue" -> "33";
            case "cloud white" -> "34";
            case "light gold" -> "35";
            case "sky blue" -> "36";
            case "cosmic orange" -> "37";
            case "deep blue" -> "38";
            case "space black" -> "39";
            default -> null;
        };
    }

    public static Integer getModelCode(String model) {
        if (model == null) return null;

        model = model.trim().toLowerCase();

        return switch (model) {
            case "iphone 5s" -> 1;
            case "iphone se" -> 2;
            case "iphone 6" -> 3;
            case "iphone 6 plus" -> 4;
            case "iphone 6s" -> 5;
            case "iphone 6s plus" -> 6;
            case "iphone 7" -> 7;
            case "iphone 7 plus" -> 8;
            case "iphone 8" -> 9;
            case "iphone 8 plus" -> 10;
            case "iphone se 2" -> 11;
            case "iphone x" -> 12;
            case "iphone xs" -> 13;
            case "iphone xs max" -> 14;
            case "iphone 11" -> 15;
            case "iphone xr" -> 16;
            case "iphone 11 pro" -> 17;
            case "iphone 11 pro max" -> 18;
            case "iphone 12 mini" -> 19;
            case "iphone 12" -> 20;
            case "iphone 12 pro" -> 21;
            case "iphone 12 pro max" -> 22;
            case "iphone 5" -> 23;
            case "iphone 13 mini" -> 24;
            case "iphone 13" -> 25;

            case "iphone se 3" -> 32;
            case "iphone 13 pro" -> 33;
            case "iphone 13 pro max" -> 34;
            case "iphone 14 pro max" -> 35;
            case "iphone 14 pro" -> 36;
            case "iphone 14" -> 37;
            case "iphone 14 plus" -> 38;
            case "iphone 15" -> 39;
            case "iphone 15 pro" -> 40;
            case "iphone 15 pro max" -> 41;
            case "iphone 15 plus" -> 42;
            case "iphone 16 plus" -> 43;
            case "iphone 16" -> 44;
            case "iphone 16 pro" -> 45;
            case "iphone 16 pro max" -> 46;
            case "iphone 16e" -> 48;
            case "iphone 17" -> 49;
            case "iphone 17 air" -> 50;
            case "iphone 17 pro" -> 51;
            case "iphone 17 pro max" -> 52;
            default -> null;
        };
    }
    public static Integer getSellerCode(String seller) {

        return switch (seller) {
            case "Twist"                         -> 1;
            case "Star trade"                    -> 2;
            case "Support"                       -> 3;
            case "Ad&Win"                        -> 4;
            case "OLX"                           -> 5;
            case "Skup wysyłkowy"                -> 6;
            case "Skup na miejscu"               -> 7;
            case "Zwrot"                         -> 8;
            case "Partly"                        -> 9;
            case "Second"                        -> 10;
            case "Simples Diagonal"              -> 11;
            case "Uein"                          -> 12;
            case "iSolutions"                    -> 13;
            case "ABC Worldwide LTD"            -> 14;
            case "Morning Smile"                 -> 15;
            case "Midas"                         -> 16;
            case "Mobico"                        -> 17;
            case "Click And Carry"               -> 18;
            case "Luxtrade"                      -> 19;
            case "WJD-REPAIRS"                   -> 20;
            case "iStock"                        -> 21;
            case "EserIF"                        -> 22;
            case "Ring Ring Distribution B.V"    -> 23;
            case "360 Greensolutions"           -> 24;
            case "AMB"                           -> 25;
            case "WELBACK (TWIST)"               -> 26;
            case "iCentrumSklep.pl"              -> 27;
            case "LikeNew"                       -> 28;
            case "GROENETELEFOON BV"             -> 29;
            case "Foxway"                        -> 30;
            case "Mobilki"                       -> 31;
            case "M.P. DYSTRYBUCJA"              -> 32;

            default -> null;
        };
    }

}
