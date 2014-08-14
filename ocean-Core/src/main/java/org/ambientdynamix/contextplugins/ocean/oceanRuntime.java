/*
 * Copyright (C) The Ambient Dynamix Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ambientdynamix.contextplugins.ocean;

import java.util.Set;
import java.util.UUID;

import android.os.Parcel;
import android.os.RemoteException;
import org.ambientdynamix.api.application.*;
import org.ambientdynamix.api.contextplugin.ContextListenerInformation;
import org.ambientdynamix.api.contextplugin.ContextPluginRuntime;
import org.ambientdynamix.api.contextplugin.ContextPluginSettings;
import org.ambientdynamix.api.contextplugin.PowerScheme;
import org.ambientdynamix.api.contextplugin.security.PrivacyRiskLevel;
import org.ambientdynamix.api.contextplugin.security.SecuredContextInfo;


import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import org.ambientdynamix.contextplugins.logger.IMyPhoneCallsInfo;


/**
 * Reactive plug-in that returns a context snapshot for the event which invoked this plugin
 *
 * @author Nirandika Wanigasekara
 */
public class oceanRuntime extends ContextPluginRuntime {
    private static final int VALID_CONTEXT_DURATION = 60000;
    // Static logging TAG
    private final String TAG = this.getClass().getSimpleName();
    // Our secure context
    private Context context;
    private StringBuffer contextSnapShot = new StringBuffer("Ocean Testing with invoke plugin v3.42");

    private DynamixFacade dynamix;
    private ContextHandler handler;
    private ContextPluginInformationResult pluginInfoResult;
    private IDynamixFacade iDynamix;

    /**
     * Called once when the ContextPluginRuntime is first initialized. The implementing subclass should acquire the
     * resources necessary to run. If initialization is unsuccessful, the plug-ins should throw an exception and release
     * any acquired resources.
     */
    @Override
    public void init(PowerScheme powerScheme, ContextPluginSettings settings) throws Exception {
        // Set the power scheme
        this.setPowerScheme(powerScheme);
        // Store our secure context
        this.context = this.getSecuredContext();
        Log.i(TAG, "Init v3.42");
    }

    /**
     * Called by the Dynamix Context Manager to start (or prepare to start) context sensing or acting operations.
     */
    @Override
    public void start() throws RemoteException {
        Log.d(TAG, "Started!");
        iDynamix = getPluginFacade().getDynamixFacade(getSessionId());
        iDynamix.openSessionWithCallback(new ISessionCallback.Stub() {
            @Override
            public void onSuccess(DynamixFacade dynamixFacade) throws RemoteException {
                //Obtain all the available plugins in the phone
                dynamix = dynamixFacade;
                Log.i(TAG, "Ocean: openSession.onSuccess");
                if (dynamixFacade != null) {
                    Log.i(TAG, "Ocean: dynamixFacade is not null");
                    //create the context handler
                    dynamixFacade.createContextHandler(new ContextHandlerCallback() {
                        @Override
                        public void onSuccess(ContextHandler contextHandler) throws RemoteException {
                            handler = contextHandler;
                            Log.i(TAG, "Ocean: createContextHandler.onSuccess");

                            //get all installed context plugin information
                            pluginInfoResult = dynamix.getInstalledContextPluginInformation();
                            if (pluginInfoResult.wasSuccessful() && pluginInfoResult != null) {
                                Log.i(TAG, "Ocean: Reached pluginInfoResult");
                                Log.i(TAG, "pluginInfoResult.getContextPluginInformation().size() " + pluginInfoResult.getContextPluginInformation().size());
                                addContextSupportForAllPlugins();
                            } else {
                                Log.w(TAG, "No plugings installed");
                            }
                        }

                        @Override
                        public void onFailure(String s, int i) throws RemoteException {
                            Log.i(TAG, "createContextHandler.onFailure " + s);
                        }
                    });

                } else {
                    Log.w(TAG, "dynamixFacade is null");
                }
            }

            @Override
            public void onFailure(String s, int i) throws RemoteException {
                Log.i(TAG, "openSession.onFailure");
            }
        });
    }

    /**
     * Called by the Dynamix Context Manager to stop context sensing or acting operations; however, any acquired
     * resources should be maintained, since start may be called again.
     */
    @Override
    public void stop() {

        Log.d(TAG, "Stopped!");
    }

    /**
     * Stops the runtime (if necessary) and then releases all acquired resources in preparation for garbage collection.
     * Once this method has been called, it may not be re-started and will be reclaimed by garbage collection sometime
     * in the indefinite future.
     */
    @Override
    public void destroy() {
        this.stop();
        context = null;
        Log.d(TAG, "Destroyed!");
    }

    @Override
    public void handleContextRequest(UUID requestId, String contextType) {

        // Check for proper context type
        if (contextType.equalsIgnoreCase(MyContextSnapShot.CONTEXT_TYPE)) {
            Log.i(TAG, "Reached the handle context request" + contextType);

            /*Invoke plugins which are not auto_reactive and receive the latest information.*/
            try {
                invokePlugins();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            // Send the context event
            //yettodo: How to pass all this context information in to the contextSnapShotInfo class?
            sendContextEvent(requestId, new SecuredContextInfo(new MyContextSnapShot(contextSnapShot),
                    PrivacyRiskLevel.LOW), VALID_CONTEXT_DURATION);

        } else {
            sendContextRequestError(requestId, "NO_CONTEXT_SUPPORT for " + contextType, ErrorCodes.CONTEXT_SUPPORT_NOT_FOUND);
        }
    }

    @Override
    public void handleConfiguredContextRequest(UUID requestId, String contextType, Bundle config) {
        // Warn that we don't handle configured requests
        Log.w(TAG, "handleConfiguredContextRequest called, but we don't support configuration!");
        // Drop the config and default to handleContextRequest
        handleContextRequest(requestId, contextType);
    }

    @Override
    public void updateSettings(ContextPluginSettings settings) {
        // Not supported
    }

    @Override
    public void setPowerScheme(PowerScheme scheme) {
        // Not supported
    }


    @Override
    public boolean addContextlistener(ContextListenerInformation listenerInfo) {
        return true;
    }

    /*
    *  invokePlugins is used to send the context request for plugins which are not AUTO_REACTIVE. The method should include a way to pass configuration bundles if needed for other context-plugins.
    *
    *  yettodo:
    *  Should narrow this method only to plugins which are not AUTO_REACTIVE?
    *  Improve this method to handle different configuration bundles for different plugins
    *  Improve the string representation to a all the formats that's possible
    *  Since getStringRepresentation() works why wouldn't other methods in the contextInfo class be accessible. Clarify with Darren. Do a test implementation and check.
    * */
    public boolean invokePlugins() throws RemoteException {
        if (!pluginInfoResult.wasSuccessful() || pluginInfoResult == null || handler == null) {
            return false;
        }
        for (ContextPluginInformation contextPlugingInfo : pluginInfoResult.getContextPluginInformation()) {
            if (contextPlugingInfo.getPluginId().equals("org.ambientdynamix.contextplugins.ocean")) {
                continue;
            }
            for (String supportedContextTypes : contextPlugingInfo.getSupportedContextTypes()) {

                Log.i(TAG, "Ocean: Reached invokePlugins()for " + contextPlugingInfo.getPluginId());
                handler.contextRequest(contextPlugingInfo.getPluginId(), supportedContextTypes, new IContextRequestCallback.Stub() {
                    @Override
                    public void onSuccess(ContextResult contextResult) throws RemoteException {
                        Log.i(TAG, "Context Request onSuccess");
                        if (contextResult.hasIContextInfo()) {
                            Log.i(TAG, "Ocean- Request id was: " + contextResult.getResponseId());
                            Log.i(TAG, "invokePlugins onSuccess: " + contextResult.getContextType());
                            IContextInfo nativeInfo = contextResult.getIContextInfo();
                            String tmp = nativeInfo.getStringRepresentation("text/plain");
                            Log.i(TAG, "Ocean PPPPPPPPPPPPPPPP:" + contextResult.getContextType() + " " + tmp);
                            getPluginTypeInfo(nativeInfo, contextResult.getContextType());
                            Log.i(TAG, "Ocean XXXXXX: ");
                        } else {
                            Log.w(TAG, "Error with context request in main loop");
                        }
                    }

                    @Override
                    public void onFailure(String s, int i) throws RemoteException {
                        Log.w(TAG, "Context Request onFailiure " + s + " " + i);
                    }
                });

            }

        }
        return true;
    }

    /*
     *When ocean needs to access context results from other plugins as its provided this method is called. It is
     * implemented for each plugin type supported by ocean.
     * The jars for the datatypes needs to be in the classpath
     * yettodo:Resolve the ClassCastException due to class loader issues.
     */
    private void getPluginTypeInfo(IContextInfo myNativeInfo, String myContextType) {

        Log.d(TAG, "DDDDD: nativeInfo Class: " + myNativeInfo.getClass());
        Log.d(TAG, "DDDDD: nativeInfo interface" + myNativeInfo.getClass().getInterfaces()[0]);
        Log.d(TAG, "DDDDD: myNativeInfo.getClass().getInterfaces()[0].getClassLoader()"+ myNativeInfo.getClass().getInterfaces()[0].getClassLoader());
        Log.d(TAG, "DDDDD: myNativeInfo.getClass().getClassLoader()" + myNativeInfo.getClass().getClassLoader());
        //Log.d(TAG, "DDDDD: myNativeInfo.getClass().getInterfaces()[0].getClassLoader()"+ myPhoneCallsInfo.getClass().getClassLoader());
        //if(nativeInfo instanceof IMyPhoneCallsInfo){
        if (myContextType.equals("org.ambientdynamix.contextplugins.logger.myphonecalls")) {
            IMyPhoneCallsInfo myPhoneCallsInfo = (IMyPhoneCallsInfo) myNativeInfo;
            Log.i(TAG, "Ocean MMMMMMMMMM: " + myPhoneCallsInfo.getMyCallLog());
        } else {
            Log.i(TAG, "Ocean LLLLLLLL: " + myContextType + " " + myNativeInfo.getImplementingClassname());
        }
    }

    /**
     * addContextSupportForAllPlugins()
     * With users' permission ocean should have the capability to install relavent/important plugins.
     * If the client does not have any context plugins installed before ocean is installed, this method will add context support to all the
     * plugins currently avaiable in the dynamix repo and call the method invokePlugins() to start receiving context information from reactive
     * plugins
     * However, if the client already has context plugins installed, ocean will not add context support for any additional context plugins,
     * respecting the users preferences.
     * yettodo: Come up with a permission scheme and a list of context plugins to be installed by ocean at this point. Write now the implementation
     * is at two extremes. i.e.either installs all or stick to the plugins already installed. Once a decision model is designed the recommended context
     * plugins should be installed at this point.
     */
    public boolean addContextSupportForAllPlugins() throws RemoteException {
        Log.d(TAG, "Ocean: Reached addContextSupportForAllPlugins");
        if(!pluginInfoResult.wasSuccessful()||pluginInfoResult==null||handler==null||dynamix==null){ //try to make this check a function
            return false;
        }
        if (pluginInfoResult.getContextPluginInformation().size() == 1) {
            pluginInfoResult = dynamix.getAllContextPluginInformation(); /*yettodo: It is better to get a list of ocean enabled plugins here, which will be a feature of each plugin dynamix has*/
            for (ContextPluginInformation contextPluginInfo : pluginInfoResult.getContextPluginInformation()) {
                Log.i(TAG, "Ocean: Reached contextPlugingInfo " + contextPluginInfo.getPluginId());

                // if (contextPlugingInfo.getPluginId().equals("org.ambientdynamix.contextplugins.logger")) {
                if (contextPluginInfo.getPluginId().equals("org.ambientdynamix.contextplugins.ocean")) {
                    continue;
                }

                for (String supportedContextTypes : contextPluginInfo.getSupportedContextTypes()) {
                    Log.i(TAG, "Ocean: supportedContextTypes " + supportedContextTypes);
                    //Add context support for each installed plugin
                    handler.addContextSupport(contextPluginInfo.getPluginId(), supportedContextTypes, contextListener, new IContextSupportCallback.Stub() {
                        @Override
                        public void onSuccess(ContextSupportInfo contextSupportInfo) throws RemoteException {
                            Log.i(TAG, "Ocean: Add context support on success " + contextSupportInfo.getContextType());
                        }

                        @Override
                        public void onProgress(int i) throws RemoteException {
                        }

                        @Override
                        public void onWarning(String s, int i) throws RemoteException {
                        }

                        @Override
                        public void onFailure(String s, int i) throws RemoteException {
                            Log.w(TAG, "Call was unsuccessful! Message: " + s + " | Error code: " + i);
                        }
                    });
                }

            }
        } else {
            Log.d(TAG, "User already has plugins installed");
        }
        invokePlugins();
        return true;
    }


    /**
     * ContextListner will be used to receive the data from AUTO_REACTIVE plugins.
     */


    private ContextListener contextListener = new ContextListener() {


        @Override
        public void onContextResult(ContextResult event) throws RemoteException {

            if (event.hasIContextInfo()) {
                Log.i(TAG, "OceanPlugin - NNNNNNNNNNNNNNN");
                Log.i(TAG, "OceanPlugin - Event contains native IContextInfo: " + event.getIContextInfo());
                IContextInfo nativeInfo = event.getIContextInfo();
                Log.i(TAG, "OceanPlugin - IContextInfo implementation class: " + nativeInfo.getImplementingClassname());
                Log.i(TAG, "OceanPlugin - onContextEvent received from plugin: " + event.getResultSource());
                Log.i(TAG, "OceanPlugin - Event context type: " + event.getContextType());
                Log.i(TAG, "OceanPlugin - Event timestamp " + event.getTimeStamp().toLocaleString());
                for (String format : event.getStringRepresentationFormats()) {
                    Log.i(TAG,
                            "Event string-based format: " + format + " contained data: "
                                    + event.getStringRepresentation(format)
                    );
                }
            }
            if (event.expires())
                Log.i(TAG, "OceanPlugin - Event expires at " + event.getExpireTime().toLocaleString());
            else
                Log.i(TAG, "OceanPlugin - Event does not expire");
        }
    };


}