package org.ambientdynamix.contextplugins.ocean;

import android.os.Bundle;
import android.util.Log;
import org.ambientdynamix.api.application.*;
import java.util.List;
import java.util.Vector;


/**
 * Created by workshop on 11/6/13.
 */
public class PluginInvoker {


    private List<PluginInvocation> pluginInvocations;

    /**
     * if this is set to true the BindDynamixActivity will not require any user interaction to run
     */
    public static final boolean AUTOMATIC_EXECUTION = false;

    private String TAG = this.getClass().getSimpleName();

    /**
     * constructor of the PluginInvoker. Add plugin Invocations here
     */
    public PluginInvoker() {
        pluginInvocations = new Vector<PluginInvocation>();

//        Add plugin invocations by calling addPluginInvocation(pluginId,context type, configuration)
        addPluginInvocation("org.ambientdynamix.contextplugins.ocean","org.ambientdynamix.contextplugins.ocean.mycontextsnapshot",null);

    }


    public List<PluginInvocation> getPluginInvocations() {
        return pluginInvocations;
    }

    /**
     * This gets called when the DynamixBindActivity receives a context event of a type specified in the cotextRequestType array
     *
     * @param event
     */
    public void invokeOnResponse(ContextResult event) {

        // Check for native IContextInfo
        if (event.hasIContextInfo()) {
            Log.i(TAG, "A1 - Event contains native IContextInfo: " + event.getIContextInfo());
            IContextInfo nativeInfo = event.getIContextInfo();
                /*
				 * Note: At this point you can cast the IContextInfo into it's native type and then call its methods. In
				 * order for this to work, you'll need to have the proper Java classes for the IContextInfo data types
				 * on your app's classpath. If you don't, event.hasIContextInfo() will return false and
				 * event.getIContextInfo() would return null, meaning that you'll need to rely on the string
				 * representation of the context info. To use native context data-types, simply download the data-types
				 * JAR for the Context Plug-in you're interested in, include the JAR(s) on your build path, and you'll
				 * have access to native context type objects instead of strings.
				 */
            Log.i(TAG, "A1 - IContextInfo implementation class: " + nativeInfo.getImplementingClassname());
            // Example of using IPedometerStepInfo

            if (nativeInfo instanceof IMyContextSnapShot) {
                IMyContextSnapShot info = (IMyContextSnapShot) nativeInfo;
                Log.i(TAG, "ContextSnapShot: " + info.getMyContextSnapShot());
            }
            // Check for other interesting types, if needed...
        } else
            Log.i(TAG,
                    "Event does NOT contain native IContextInfo... we need to rely on the string representation!");
    }

    /**
     *
     * @param pluginId    The plugin to call
     * @param contextType  The contextType  to request
     * @param configuration  optional configuration bundle, can be null
     */
    private void addPluginInvocation(String pluginId, String contextType, Bundle configuration) {
        pluginInvocations.add(new PluginInvocation(pluginId,contextType,configuration));
    }

    public class PluginInvocation {
        private String pluginId;
        private String contextRequestType;
        private Bundle configuration;
        private boolean successfullyCalled;

        public PluginInvocation(String pluginId, String contextRequestType, Bundle configuration) {
            this.pluginId = pluginId;
            this.contextRequestType = contextRequestType;
            this.configuration = configuration;
        }

        public String getPluginId() {
            return pluginId;
        }

        public void setPluginId(String pluginId) {
            this.pluginId = pluginId;
        }

        public String getContextRequestType() {
            return contextRequestType;
        }

        public void setContextRequestType(String contextRequestType) {
            this.contextRequestType = contextRequestType;
        }

        public Bundle getConfiguration() {
            return configuration;
        }

        public void setConfiguration(Bundle configuration) {
            this.configuration = configuration;
        }

        public boolean isSuccessfullyCalled() {
            return successfullyCalled;
        }

        public void setSuccessfullyCalled(boolean successfullyCalled) {
            this.successfullyCalled = successfullyCalled;
        }
    }

}