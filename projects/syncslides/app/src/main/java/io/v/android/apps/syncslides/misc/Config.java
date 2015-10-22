// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.misc;

import io.v.android.apps.syncslides.model.Deck;

/**
 * Syncslides configuration.
 */
public class Config {
    /**
     * Which Mounttable to use for everything.
     */
    public static final String MT_ADDRESS = Tables.PI_MILK_CRATE;

    /**
     * Some fixed mount tables to try.
     */
    private static class Tables {
        static final String PI_MILK_CRATE = "192.168.86.254:8101";
        static final String JR_LAPTOP_AT_HOME = "192.168.2.71:23000";
        static final String JR_LAPTOP_VEYRON = "192.168.8.106:23000";
        static final String JR_MOTOX = "192.168.43.136:23000";
    }

    public static class Syncbase {
        /**
         * If true, enable use of syncbase as the DB, else use a fake.
         */
        public static final boolean ENABLE = false;
    }

    public static class MtDiscovery {
        /**
         * If true, enable MT-based (mounttable) discovery, else use a fake. If
         * enabled, DeckChooserActivity will scan a MT to find live
         * presentations. Clicking play on a presentation will start a service
         * and try to mount it in a MT so other deck views can list it in the
         * UX.
         */
        public static final boolean ENABLE = true;
        /**
         * Every v23 service will be mounted in the namespace with a name
         * prefixed by this.
         */
        private static String LIVE_PRESENTATION_PREFIX = "powerDeck";

        /**
         * TODO(jregan): Assure legal mount name (remove blanks and such).
         */
        public static String makeMountName(Deck deck) {
            return LIVE_PRESENTATION_PREFIX + "/" + deck.getId();
        }

        public static String makeScanString() {
            return LIVE_PRESENTATION_PREFIX + "/*";
        }
    }
}
