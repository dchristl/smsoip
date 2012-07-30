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
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.ui.BreakingProgressDialog;
import org.acra.ErrorReporter;

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
                ErrorReporter.getInstance().putCustomData("LAST ACTION", "getFireSMSAndUpdateUIRunnable");
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
                                        CharSequence infoText = null;
                                        //wait until the sending has finished
                                        FireSMSResultList fireSMSResultList = builder.getFireSMSResults();
                                        FireSMSResultList.SendResult sendResult = fireSMSResultList.getResult();
                                        if (sendResult == FireSMSResultList.SendResult.BOTH || sendResult == FireSMSResultList.SendResult.SUCCESS) { //success or both
                                            SMSActionResult refreshResult = RunnableFactory.this.sendActivity.refreshInformationText(true);
                                            if (refreshResult.isSuccess()) {
                                                infoText = refreshResult.getMessage();
                                            }
                                        }
                                        updateUIHandler.post(getUpdateUIRunnable(fireSMSResultList, infoText == null ? null : infoText.toString()));
                                        RunnableFactory.this.progressDialog.cancel();
                                    }
                                });

                                builder.show();
                            }
                        });
                    } else {
                        CharSequence infoText = null;
                        FireSMSResultList.SendResult sendResult = fireSMSResults.getResult();
                        if (sendResult == FireSMSResultList.SendResult.BOTH || sendResult == FireSMSResultList.SendResult.SUCCESS) { //success or both
                            SMSActionResult refreshResult = RunnableFactory.this.sendActivity.refreshInformationText(true);
                            if (refreshResult.isSuccess()) {
                                infoText = refreshResult.getMessage();
                            }
                        }
                        updateUIHandler.post(getUpdateUIRunnable(fireSMSResults, infoText == null ? null : infoText.toString()));
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
     * @param infoText
     * @return
     */
    private Runnable getUpdateUIRunnable(final FireSMSResultList fireSMSResults, final String infoText) {
        return new Runnable() {
            public void run() {
                ErrorReporter.getInstance().putCustomData("LAST ACTION", "getUpdateUIRunnable");
                RunnableFactory.this.sendActivity.showReturnMessage(fireSMSResults, infoText);
            }
        };
    }


    /**
     * since API level 14
     */
    public Runnable getRefreshAndUpdateUIRunnable() {
        return new Runnable() {
            public void run() {
                ErrorReporter.getInstance().putCustomData("LAST ACTION", "getRefreshAndUpdateUIRunnable");
                final SMSActionResult result = RunnableFactory.this.sendActivity.refreshInformationText(false);
                Runnable runnable = new Runnable() {
                    public void run() {
                        RunnableFactory.this.sendActivity.updateInfoTextThroughRefresh(result);
                    }
                };
                updateUIHandler.post(runnable);
                progressDialog.cancel();
            }
        };
    }

    public void updateInfoTextInBackground() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                ErrorReporter.getInstance().putCustomData("LAST ACTION", "updateInfoTextInBackground");
                SMSActionResult actionResult = sendActivity.refreshInformationText(false);
                if (actionResult.isSuccess()) {
                    final String infoText = actionResult.getMessage();
                    Runnable runnable = new Runnable() {
                        public void run() {
                            sendActivity.updateInfoTextAndRefreshButton(infoText);
                        }
                    };
                    updateUIHandler.post(runnable);
                } else {
                    updateUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            sendActivity.updateInfoTextAndRefreshButton(null);

                        }
                    });
                }

            }
        });
        thread.start();
    }
}
