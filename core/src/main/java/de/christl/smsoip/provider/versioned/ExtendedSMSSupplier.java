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

package de.christl.smsoip.provider.versioned;

import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.annotations.APIVersion;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.option.OptionProvider;

import java.util.List;

@APIVersion(minVersion = 14)
public interface ExtendedSMSSupplier {

    /**
     * HTTP Useragent.
     */
    final String TARGET_AGENT = "Mozilla/3.0 (compatible)";
    /**
     * TIMEOUT can be used for any connection
     */
    final int TIMEOUT = 10000;

    /**
     * will be called on send SMS button
     *
     * @param smsText     - the message text
     * @param receivers   - indexed map of all receivers
     * @param spinnerText - the text of the spinner or null if not visible  @return Result.NO_ERRORS on ic_menu_success or any other otherwise
     */

    FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText);

    /**
     * will be called by checking the login in the option dialog
     *
     * @param userName - the username
     * @param password - the password
     * @return Result.NO_ERRORS -> Strings will be marked green, otherwise red
     */
    SMSActionResult checkCredentials(String userName, String password);

    /**
     * this will called when the refresh button is used, is splitted by refreshInformationAfterMessageSuccessfulSent
     * for possibility to increase performance
     * (e.g. call login twice)
     *
     * @return Result.NO_ERRORS add additional info for the informations next to refresh button<br/>
     *         any other error if not successful
     */
    SMSActionResult refreshInfoTextOnRefreshButtonPressed();

    /**
     * this will called after message is sent successful, is splitted by refreshInformationOnRefreshButtonPressed
     * for possibility to increase performance
     * (e.g. call login twice on message sent and on refresh)
     *
     * @return Result.NO_ERRORS add additional info for the informations next to refresh button<br/>
     *         any other error if not successful
     */
    SMSActionResult refreshInfoTextAfterMessageSuccessfulSent();

    /**
     * the corresponding provider of this supplier
     *
     * @return corresponding provider
     */
    OptionProvider getProvider();
}
