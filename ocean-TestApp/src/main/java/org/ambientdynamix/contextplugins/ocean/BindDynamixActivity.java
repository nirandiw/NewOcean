package org.ambientdynamix.contextplugins.ocean;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import org.ambientdynamix.api.application.*;
import org.ambientdynamix.contextplugins.ocean.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by workshop on 11/4/13.
 */
public class BindDynamixActivity extends Activity {
    private final String TAG = this.getClass().getSimpleName();
    private DynamixFacade dynamix;
    private PluginInvoker pluginInvoker;
    private ContextHandler contextHandler;

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the
     *                           data it most recently supplied in onSaveInstanceState(Bundle). <b>Note: Otherwise it is null.</b>
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pluginInvoker = new PluginInvoker();
        if (PluginInvoker.AUTOMATIC_EXECUTION) {
            setContentView(R.layout.main_auto);
            connect();
        } else {
            setContentView(R.layout.main);
            Button btnConnect = (Button) findViewById(R.id.btnConnect);
            btnConnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    connect();
                }
            });
            // Setup the disconnect button
            Button btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
            btnDisconnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    disconnect();
                }
            });
            /*
			* Setup the interactive context acquisition button. Note that this method only works if the
			* 'org.ambientdynamix.sampleplugin' plug-in is installed.
		 	*/
            Button btnInvokePlugin = (Button) findViewById(R.id.invokePlugin);
            btnInvokePlugin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    invokePlugins();
                }
            });
        }
    }

    private void invokePlugins() {
        /**
         * This gets called once the user presses the invoke plugins button on the device. The deafult behaviour is to
         * request all context Types listed in the cotextRequestType Array
         */
        if (contextHandler != null) {
            try {
                Log.i(TAG, "A1 - Requesting Programmatic Context Acquisitions");
                for (PluginInvoker.PluginInvocation pluginInvocation : pluginInvoker.getPluginInvocations()) {
                    if (pluginInvocation.getConfiguration() != null) {

                        contextHandler.contextRequest(pluginInvocation.getPluginId(),
                                pluginInvocation.getContextRequestType(), pluginInvocation.getConfiguration(), new IContextRequestCallback.Stub() {
                            @Override
                            public void onSuccess(ContextResult contextEvent) throws RemoteException {
                                Log.i(TAG, "A1 - Request id was: " + contextEvent.getResponseId());
                                contextListener.onContextResult(contextEvent);
                            }

                            @Override
                            public void onFailure(String s, int i) throws RemoteException {
                                Log.w(TAG, "Call was unsuccessful! Message: " + s + " | Error code: "
                                        + i);
                            }

                        });
                    } else {
                        contextHandler.contextRequest(pluginInvocation.getPluginId(),
                                pluginInvocation.getContextRequestType(), new IContextRequestCallback.Stub() {
                            @Override
                            public void onSuccess(ContextResult contextEvent) throws RemoteException {
                                Log.i(TAG, "A1 - Request id was: " + contextEvent.getResponseId());
                                contextListener.onContextResult(contextEvent);
                            }

                            @Override
                            public void onFailure(String s, int i) throws RemoteException {
                                Log.w(TAG, "Call was unsuccessful! Message: " + s + " | Error code: "
                                        + i);
                            }
                        });
                    }

                }
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        } else
            Log.w(TAG, "Dynamix not connected.");
    }

    private void disconnect() {
        initializing = false;
        if (contextHandler != null) {
            try {
                /*
                 * In this example, this Activity controls the session, so we call closeSession here. This will
                 * close the session for ALL of the application's IDynamixListeners.
                 */
                dynamix.closeSession(new Callback() {
                    @Override
                    public void onFailure(String message, int errorCode) throws RemoteException {
                        Log.w(TAG, "Call was unsuccessful! Message: " + message + " | Error code: "
                                + errorCode);
                    }

                    @Override
                    public void onSuccess() throws RemoteException {
                        Log.w(TAG, "Session closed");
                    }
                });
                dynamix = null;
                contextHandler = null;

            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    private void connect() {
        if (contextHandler == null) {
            try {
                DynamixConnector.openConnection(this, true, null, new ISessionCallback.Stub() {
                    @Override
                    public void onSuccess(DynamixFacade iDynamixFacade) throws RemoteException {
                        dynamix = iDynamixFacade;
                        dynamix.createContextHandler(new ContextHandlerCallback.Stub() {

                            @Override
                            public void onSuccess(ContextHandler handler) throws RemoteException {
                                contextHandler = handler;
                                init(pluginInvoker, new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            registerForContextTypes();
                                        } catch (RemoteException e) {
                                            e.printStackTrace();
                                        }
                                        //TODO This changed
                                    }
                                });

                            }

                            @Override
                            public void onFailure(String message, int errorCode) throws RemoteException {
                                Log.e(TAG,message);
                            }
                        });//

                    }

                    @Override
                    public void onFailure(String s, int i) throws RemoteException {

                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }else
            try {
                registerForContextTypes();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    private ContextListener contextListener = new ContextListener() {


        @Override
        public void onContextResult(ContextResult event) throws RemoteException {
            /*
             * Log some information about the incoming event
			 */
            Log.i(TAG, "A1 - onContextEvent received from plugin: " + event.getResultSource());
            Log.i(TAG, "A1 - -------------------");
            Log.i(TAG, "A1 - Event context type: " + event.getContextType());
            Log.i(TAG, "A1 - Event timestamp " + event.getTimeStamp().toLocaleString());
            if (event.expires())
                Log.i(TAG, "A1 - Event expires at " + event.getExpireTime().toLocaleString());
            else
                Log.i(TAG, "A1 - Event does not expire");
			/*
			 * To illustrate how string-based context representations are accessed, we log each contained in the event.
			 */
            for (String format : event.getStringRepresentationFormats()) {
                Log.i(TAG,
                        "Event string-based format: " + format + " contained data: "
                                + event.getStringRepresentation(format));
            }
            boolean done = true;
            for (PluginInvoker.PluginInvocation pluginInvocation : pluginInvoker.getPluginInvocations()) {
                if (pluginInvocation.getContextRequestType().equals(event.getContextType())) {
                    pluginInvocation.setSuccessfullyCalled(true);
                    pluginInvoker.invokeOnResponse(event);

                }
                if (!pluginInvocation.isSuccessfullyCalled())
                    done = false;
            }

            if (done && PluginInvoker.AUTOMATIC_EXECUTION) {
                disconnect();
            }
        }
    };


    private List<ContextPluginInformation> uninstallingPlugins = new ArrayList<ContextPluginInformation>();

    private boolean initializing;

    /**
     * Threaded initialization that automatically uninstalls the plug-ins targeted by the pluginInvoker, waiting for
     * Dynamix events as needed.
     *
     * @param pluginInvoker The PluginInvoker of target plug-ins
     * @param callback      An (optional) callback to run after initialization
     * @throws RemoteException
     */
    private synchronized void init(final PluginInvoker pluginInvoker, final Runnable callback) throws RemoteException {
        // Check for init state
        if (!initializing) {
            Log.i(TAG, "Init running");
            initializing = true;
            // Create a list of targetPluginIds
            List<String> targetPluginIds = new ArrayList<String>();
            for (PluginInvoker.PluginInvocation invocation : pluginInvoker.getPluginInvocations())
                targetPluginIds.add(invocation.getPluginId());
            // Clear the uninstall list
            uninstallingPlugins.clear();
            // Access the list of installed Dynamix plug-ins
            ContextPluginInformationResult result = dynamix.getAllContextPluginInformation();
            if (result.wasSuccessful()) {
                // Add the ContextPluginInformation for each target plug-in to the uninstallingPlugins list
                for (ContextPluginInformation plug : result.getContextPluginInformation()) {
                    for (String id : targetPluginIds) {
                        if (plug.getPluginId().equalsIgnoreCase(id))
                            uninstallingPlugins.add(plug);
                    }
                }
                if (!uninstallingPlugins.isEmpty()) {
                    final int[] count = new int[1];
                    final int total = uninstallingPlugins.size();
                    for (final ContextPluginInformation info : uninstallingPlugins) {
                        Log.i(TAG, "Calling uninstall for plug " + info);
                        try {
                            dynamix.requestContextPluginUninstall(info, new Callback() {
                                @Override
                                public void onSuccess() throws RemoteException {
                                    Log.i(TAG, "Uninstalled: " + info.getPluginId());
                                    count[0]++;
                                    if (count[0]==total) {
                                        Log.i(TAG, "Waiting for uninstall to complete... FINISHED!");
                                        // Set init to false, since we're finished
                                        initializing = false;
                                        // Fire the callback, if necessary (i.e, if it's not null)
                                        if (callback != null)
                                            callback.run();
                                    }
                                }

                                @Override
                                public void onFailure(String message, int errorCode) throws RemoteException {
                                    Log.w(TAG, "Uninstalling: " + info.getPluginId() + " failed, reason: " + message + " moving on as if uninstall succeeded");
                                    count[0]++;
                                    if (count[0]==total) {
                                        Log.i(TAG, "Waiting for uninstall to complete... FINISHED!");
                                        // Set init to false, since we're finished
                                        initializing = false;
                                        // Fire the callback, if necessary (i.e, if it's not null)
                                        if (callback != null)
                                            callback.run();
                                    }
                                }
                            });
                        } catch (RemoteException e) {
                            Log.w(TAG, e);
                        }
                    }
                    uninstallingPlugins.clear();

                } else {
                    Log.i(TAG, "No plug-ins to uninstall!");
                    // Set init to false, since we're finished
                    initializing = false;
                    // Fire the callback, if necessary (i.e, if it's not null)
                    if (callback != null)
                        callback.run();
                }
            } else
                Log.w(TAG, "Could not access plug-in information from Dynamix");
        } else
            Log.w(TAG, "Already initializing, please wait...");
    }

    /*
	 * Utility method that registers for the context types needed by this class.
	 */
    private void registerForContextTypes() throws RemoteException {
		/*
		 * Below are several examples of adding context support using context types. In this case, the plug-in (or
		 * plug-ins) assigned to handle context type will be automatically selected by Dynamix.
		 */
        for (PluginInvoker.PluginInvocation pluginInvocation : pluginInvoker.getPluginInvocations()) {
            String type = pluginInvocation.getContextRequestType();
            contextHandler.addContextSupport(pluginInvocation.getPluginId(),type, contextListener, new ContextSupportCallback() {
                @Override
                public void onSuccess(ContextSupportInfo supportInfo) throws RemoteException {
                    Log.i(TAG,"support succesfully added");
                }

                @Override
                public void onFailure(String message, int errorCode) throws RemoteException {
                    Log.w(TAG,
                            "Call was unsuccessful! Message: " + message + " | Error code: "
                                    + errorCode);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "ON DESTROY for: Dynamix Simple Logger (A1)");
		/*
		 * Always remove our listener and unbind so we don't leak our service connection
		 */
        if (contextHandler != null) {
//            try {
////                dynamix.closeSession();
//
//                contextHandler = null;
//            } catch (RemoteException e) {
//            }
        }
        super.onDestroy();
    }
}
