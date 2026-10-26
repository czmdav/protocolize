package dev.simplix.protocolize.api.util;

import java.util.Map;

/**
 * This class contains protocol constants.
 * <p>
 * Protocolize supports Minecraft 1.8.x (item mappings via the legacy data module) and
 * Minecraft {@link #MINECRAFT_1_21} up to {@link #MINECRAFT_LATEST}. The constants of the versions
 * in between are only kept as protocol number references and are not supported anymore.
 */
public final class ProtocolVersions {

    public final static int MINECRAFT_1_8 = 47;
    public final static int MINECRAFT_1_9 = 107;
    public final static int MINECRAFT_1_9_1 = 108;
    public final static int MINECRAFT_1_9_2 = 109;
    public final static int MINECRAFT_1_9_3 = 110;
    public final static int MINECRAFT_1_9_4 = MINECRAFT_1_9_3;
    public final static int MINECRAFT_1_10 = 210;
    public final static int MINECRAFT_1_11 = 315;
    public final static int MINECRAFT_1_11_1 = 316;
    public final static int MINECRAFT_1_11_2 = MINECRAFT_1_11_1;
    public final static int MINECRAFT_1_12 = 335;
    public final static int MINECRAFT_1_12_1 = 338;
    public final static int MINECRAFT_1_12_2 = 340;
    public final static int MINECRAFT_1_13 = 393;
    public final static int MINECRAFT_1_13_1 = 401;
    public final static int MINECRAFT_1_13_2 = 404;
    public final static int MINECRAFT_1_14 = 477;
    public final static int MINECRAFT_1_14_1 = 480;
    public final static int MINECRAFT_1_14_2 = 485;
    public final static int MINECRAFT_1_14_3 = 490;
    public final static int MINECRAFT_1_14_4 = 498;
    public final static int MINECRAFT_1_15 = 573;
    public final static int MINECRAFT_1_15_1 = 575;
    public final static int MINECRAFT_1_15_2 = 578;
    public final static int MINECRAFT_1_16 = 735;
    public final static int MINECRAFT_1_16_1 = 736;
    public final static int MINECRAFT_1_16_2 = 751;
    public final static int MINECRAFT_1_16_3 = 753;
    public final static int MINECRAFT_1_16_4 = 754;
    public final static int MINECRAFT_1_16_5 = MINECRAFT_1_16_4;
    public final static int MINECRAFT_1_17 = 755;
    public final static int MINECRAFT_1_17_1 = 756;
    public final static int MINECRAFT_1_18 = 757;
    public final static int MINECRAFT_1_18_1 = MINECRAFT_1_18;
    public final static int MINECRAFT_1_18_2 = 758;
    public final static int MINECRAFT_1_19 = 759;
    public final static int MINECRAFT_1_19_1 = 760;
    public final static int MINECRAFT_1_19_2 = MINECRAFT_1_19_1;
    public final static int MINECRAFT_1_19_3 = 761;
    public final static int MINECRAFT_1_19_4 = 762;
    public final static int MINECRAFT_1_20 = 763;
    public final static int MINECRAFT_1_20_1 = MINECRAFT_1_20;
    public final static int MINECRAFT_1_20_2 = 764;
    public final static int MINECRAFT_1_20_3 = 765;
    public final static int MINECRAFT_1_20_4 = MINECRAFT_1_20_3;
    public final static int MINECRAFT_1_20_5 = 766;
    public final static int MINECRAFT_1_20_6 = MINECRAFT_1_20_5;
    public final static int MINECRAFT_1_21 = 767;
    public final static int MINECRAFT_1_21_1 = MINECRAFT_1_21;
    public final static int MINECRAFT_1_21_2 = 768;
    public final static int MINECRAFT_1_21_3 = MINECRAFT_1_21_2;
    public final static int MINECRAFT_1_21_4 = 769;
    public final static int MINECRAFT_1_21_5 = 770;
    public final static int MINECRAFT_1_21_6 = 771;
    public final static int MINECRAFT_1_21_7 = 772;
    public final static int MINECRAFT_1_21_8 = MINECRAFT_1_21_7;
    public final static int MINECRAFT_1_21_9 = 773;
    public final static int MINECRAFT_1_21_10 = MINECRAFT_1_21_9;
    public final static int MINECRAFT_1_21_11 = 774;
    public final static int MINECRAFT_26_1 = 775;
    public final static int MINECRAFT_26_1_1 = MINECRAFT_26_1;
    public final static int MINECRAFT_26_1_2 = MINECRAFT_26_1;
    public final static int MINECRAFT_26_2 = 776;
    public final static int MINECRAFT_26_3 = 777;
    public final static int MINECRAFT_LATEST = MINECRAFT_26_3;

    /**
     * Every official Minecraft release id (key) with the protocol number (value) it speaks.
     * Several releases can share one protocol number, so the release id is the key.
     * The official-jar verification test downloads the server jar of each release listed here.
     */
    public final static Map<String, Integer> OFFICIAL_RELEASES = Map.ofEntries(
        Map.entry("1.13", MINECRAFT_1_13),
        Map.entry("1.13.1", MINECRAFT_1_13_1),
        Map.entry("1.13.2", MINECRAFT_1_13_2),
        Map.entry("1.14", MINECRAFT_1_14),
        Map.entry("1.14.1", MINECRAFT_1_14_1),
        Map.entry("1.14.2", MINECRAFT_1_14_2),
        Map.entry("1.14.3", MINECRAFT_1_14_3),
        Map.entry("1.14.4", MINECRAFT_1_14_4),
        Map.entry("1.15", MINECRAFT_1_15),
        Map.entry("1.15.1", MINECRAFT_1_15_1),
        Map.entry("1.15.2", MINECRAFT_1_15_2),
        Map.entry("1.16", MINECRAFT_1_16),
        Map.entry("1.16.1", MINECRAFT_1_16_1),
        Map.entry("1.16.2", MINECRAFT_1_16_2),
        Map.entry("1.16.3", MINECRAFT_1_16_3),
        Map.entry("1.16.4", MINECRAFT_1_16_4),
        Map.entry("1.16.5", MINECRAFT_1_16_5),
        Map.entry("1.17", MINECRAFT_1_17),
        Map.entry("1.17.1", MINECRAFT_1_17_1),
        Map.entry("1.18", MINECRAFT_1_18),
        Map.entry("1.18.1", MINECRAFT_1_18_1),
        Map.entry("1.18.2", MINECRAFT_1_18_2),
        Map.entry("1.19", MINECRAFT_1_19),
        Map.entry("1.19.1", MINECRAFT_1_19_1),
        Map.entry("1.19.2", MINECRAFT_1_19_2),
        Map.entry("1.19.3", MINECRAFT_1_19_3),
        Map.entry("1.19.4", MINECRAFT_1_19_4),
        Map.entry("1.20", MINECRAFT_1_20),
        Map.entry("1.20.1", MINECRAFT_1_20_1),
        Map.entry("1.20.2", MINECRAFT_1_20_2),
        Map.entry("1.20.3", MINECRAFT_1_20_3),
        Map.entry("1.20.4", MINECRAFT_1_20_4),
        Map.entry("1.20.5", MINECRAFT_1_20_5),
        Map.entry("1.20.6", MINECRAFT_1_20_6),
        Map.entry("1.21", MINECRAFT_1_21),
        Map.entry("1.21.1", MINECRAFT_1_21_1),
        Map.entry("1.21.2", MINECRAFT_1_21_2),
        Map.entry("1.21.3", MINECRAFT_1_21_3),
        Map.entry("1.21.4", MINECRAFT_1_21_4),
        Map.entry("1.21.5", MINECRAFT_1_21_5),
        Map.entry("1.21.6", MINECRAFT_1_21_6),
        Map.entry("1.21.7", MINECRAFT_1_21_7),
        Map.entry("1.21.8", MINECRAFT_1_21_8),
        Map.entry("1.21.9", MINECRAFT_1_21_9),
        Map.entry("1.21.10", MINECRAFT_1_21_10),
        Map.entry("1.21.11", MINECRAFT_1_21_11),
        Map.entry("26.1", MINECRAFT_26_1),
        Map.entry("26.1.1", MINECRAFT_26_1_1),
        Map.entry("26.1.2", MINECRAFT_26_1_2),
        Map.entry("26.2", MINECRAFT_26_2),
        Map.entry("26.3", MINECRAFT_26_3)
    );

    private ProtocolVersions() {
    }

}
