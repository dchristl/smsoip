/*
 * Copyright (c) Danny Christl 2012.
 *     This file is part of SMSoIP.
 *
 *     SMSoIP is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     SMSoIP is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with SMSoIP.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.christl.smsoip.activities.settings;

/**
 * Class for all constants
 * helps to encapsulate from GlobalPreferences
 */
public abstract class SettingsConst {


    private SettingsConst() {
    }

    public static final String GLOBAL_BUTTON_VISIBILITY = "global.button.visibility";

    public static final String GLOBAL_DEFAULT_PROVIDER = "global.default.provider";

    public static final String GLOBAL_AREA_CODE = "global.area.code";
    public static final String GLOBAL_ENABLE_NETWORK_CHECK = "global.enable.network.check";
    public static final String GLOBAL_ENABLE_INFO_UPDATE_ON_STARTUP = "global.update.info.startup";
    public static final String GLOBAL_ENABLE_COMPACT_MODE = "global.compact.mode";
    public static final String GLOBAL_ENABLE_PROVIDER_OUPUT = "global.enable.provider.output";
    public static final String GLOBAL_WRITE_TO_DATABASE = "global.write.to.database";
    public static final String GLOBAL_FONT_SIZE_FACTOR = "global.font.size.factor";
    public static final String EXTRA_ADJUSTMENT = "extra.adjustment";
    public static final String RECEIVER_ACTIVATED = "receiver.activated";
    public static final String RECEIVER_ONLY_ONE_NOTFICATION = "receiver.only.one.notification";
    public static final String RECEIVER_SHOW_DIALOG = "receiver.show.dialog";

    public static final String RECEIVER_RINGTONE_URI = "receiver.ringtone";
    public static final String RECEIVER_VIBRATE_ACTIVATED = "receiver.vibrate.activated";
    public static final String RECEIVER_LED_ACTIVATED = "receiver.led.activated";
    public static final String SERIAL = "registered.serial";

    public static final String TEXT_MODULES_PREFIX = "text.module.";

    public static final String CONVERSATION_COUNT = "extra.conversation.count";
    public static final String CONVERSATION_ORDER = "extra.conversation.order";
    public static final String OUTPUT_TEMPLATE_MULTI = "output.template.multi";
    public static final String OUTPUT_TEMPLATE_SINGLE = "output.template.single";

    public static final String INCOMING_COLOR = "extra.incoming.color";
    public static final String INCOMING_TEXT_COLOR = "extra.incoming.text.color";
    public static final String OUTGOING_COLOR = "extra.outgoing.color";
    public static final String OUTGOING_TEXT_COLOR = "extra.outgoing.text.color";

    public static final String REFRESH_CACHE = "receiver.refresh.cache";
    public static final String PREFER_INTERSTITIAL = "prefer.interstitial";

}
