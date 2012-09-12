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

package de.christl.smsoip.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.util.Log;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.LogConst;
import de.christl.smsoip.models.ErrorReporterStack;
import de.christl.smsoip.ui.BreakingProgressDialog;

import java.util.concurrent.*;

/**
 * little helper for building updating the ui after sending or refreshing informations
 */
public class RunnableFactory {


    private SendActivity sendActivity;
    private ProgressDialog progressDialog;
    private Handler updateUIHandler;


    RunnableFactory(SendActivity sendActivity, ProgressDialog progressDialog) {
        this.sendActivity = sendActivity;
        this.progressDialog = progressDialog;
        updateUIHandler = new Handler();
    }


    /**
     * available since API level 14
     *
     * @return
     */
    public Runnable getFireSMSAndUpdateUIRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                ErrorReporterStack.put(LogConst.GET_FIRE_SMS_AND_UPDATE_UI_RUNNABLE);
                ExecutorService executorService = Executors.newFixedThreadPool(2);
                Callable<FireSMSResultList> runnable = new Callable<FireSMSResultList>() {
                    @Override
                    public FireSMSResultList call() throws Exception {
                        return RunnableFactory.this.sendActivity.sendByThread();
                    }
                };

                Future<FireSMSResultList> future = executorService.submit(runnable);

                FireSMSResultList fireSMSResults = null;
                try {
                    fireSMSResults = future.get();
                } catch (InterruptedException e) {
                    Log.e("christl", "", e);
                } catch (ExecutionException e) {
                    Log.e("christl", "", e);
                }

                if (fireSMSResults != null) {

                    if (fireSMSResults.getResult().equals(FireSMSResultList.SendResult.DIALOG)) {
                        final BreakingProgressDialog builder = fireSMSResults.getBuilder();
                        updateUIHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                builder.setListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        //wait until the sending has finished
                                        FireSMSResultList fireSMSResultList = builder.getFireSMSResults();
                                        FireSMSResultList.SendResult sendResult = fireSMSResultList.getResult();
                                        if (sendResult == FireSMSResultList.SendResult.BOTH || sendResult == FireSMSResultList.SendResult.SUCCESS) { //success or both
                                            RunnableFactory.this.sendActivity.refreshInformationText();
                                        }
                                        updateUIHandler.post(getUpdateUIRunnable(fireSMSResultList));
                                        RunnableFactory.this.progressDialog.cancel();
                                    }
                                });

                                builder.show();
                            }
                        });
                    } else {
                        FireSMSResultList.SendResult sendResult = fireSMSResults.getResult();
                        if (sendResult == FireSMSResultList.SendResult.BOTH || sendResult == FireSMSResultList.SendResult.SUCCESS) { //success or both
                            RunnableFactory.this.sendActivity.refreshInformationText();
                        }
                        updateUIHandler.post(getUpdateUIRunnable(fireSMSResults));
                        RunnableFactory.this.progressDialog.cancel();
                    }

                }

            }
        };
    }

    /**
     * since API level 14
     *
     * @param fireSMSResults
     * @return
     */
    private Runnable getUpdateUIRunnable(final FireSMSResultList fireSMSResults) {
        return new Runnable() {
            public void run() {
                ErrorReporterStack.put(LogConst.GET_UPDATE_UI_RUNNABLE);
                RunnableFactory.this.sendActivity.showReturnMessage(fireSMSResults);
            }
        };
    }


}
