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

package de.christl.smsoip.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.XmlResourceParser;
import android.view.LayoutInflater;
import android.view.View;

import org.acra.ACRA;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Class for constructing dialog for showing during check credentials or fire SMS phase
 */
public final class BreakingProgressDialogFactory<T> {

    private BreakingProgressDialogDismissListener<T> listener;
    private Context context;
    private CharSequence positiveButtonText;
    private CharSequence negativeButtonText;
    private boolean isCancelable = true;
    private Future<T> futureResult;
    private String title;
    private XmlResourceParser xmlLayout;
    View layout;


    public void setPositiveButtonText(String positiveButtonText) {
        this.positiveButtonText = positiveButtonText;
    }

    public void setNegativeButtonText(String negativeButtonText) {
        this.negativeButtonText = negativeButtonText;
    }

    public void setListener(BreakingProgressDialogDismissListener<T> listener) {
        this.listener = listener;
    }

    public void setCancelable(boolean cancelable) {
        isCancelable = cancelable;
    }

    public void setXmlLayout(XmlResourceParser xmlLayout) {
        this.xmlLayout = xmlLayout;
    }

    public AlertDialog create() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        layout = LayoutInflater.from(context).inflate(xmlLayout, null);
        builder.setView(layout);
        AlertDialog alertDialog = builder.create();
        final ExecutorService executorService = Executors.newFixedThreadPool(1);
        if (positiveButtonText != null) {
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, positiveButtonText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Callable<T> runnable = new Callable<T>() {
                        @Override
                        public T call() throws Exception {
                            return listener.onPositiveButtonClicked();
                        }
                    };
                    futureResult = executorService.submit(runnable);
                    try {
                        dialog.dismiss();
                    } catch (IllegalArgumentException e) {
                        ACRA.getErrorReporter().handleSilentException(e);
                    }
                }
            });
        }

        if (negativeButtonText != null) {
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, negativeButtonText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Callable<T> runnable = new Callable<T>() {
                        @Override
                        public T call() throws Exception {
                            return listener.onNegativeButtonClicked();
                        }
                    };
                    futureResult = executorService.submit(runnable);
                    try {
                        dialog.dismiss();
                    } catch (IllegalArgumentException e) {
                        ACRA.getErrorReporter().handleSilentException(e);
                    }

                }
            });
        }

        if (isCancelable) {
            alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    Callable<T> runnable = new Callable<T>() {
                        @Override
                        public T call() throws Exception {
                            return listener.onCancel();
                        }
                    };
                    futureResult = executorService.submit(runnable);
                    try {
                        dialog.dismiss();
                    } catch (IllegalArgumentException e) {
                        ACRA.getErrorReporter().handleSilentException(e);
                    }
                }
            });
        } else {
            alertDialog.setCancelable(false);
        }
        listener.afterDialogCreated();
        return alertDialog;
    }

    public AlertDialog create(Context context) {
        this.context = context;
        return create();
    }

    public T getFutureResult() {
        try {
            return futureResult.get();
        } catch (InterruptedException ignored) {
        } catch (ExecutionException ignored) {
        }
        return null;
    }

    /**
     * get the view inside the layout
     * <b>only callable after create is called</b>
     *
     * @return
     */
    public View getLayout() {
        return layout;
    }
}
