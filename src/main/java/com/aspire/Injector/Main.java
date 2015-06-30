package com.aspire.Injector;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link Main} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #filePath})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked. 
 *
 * @author Ayman Alkhalil
 */
public class Main extends Builder {

    private final String filePath;
    private final String injectedProperties;
    
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public Main(String filePath, String injectedProperties) {
        this.filePath = filePath;
        this.injectedProperties = injectedProperties;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getFilePath() {
        return filePath;
    }
    
    public String getInjectedProperties() {
        return injectedProperties;
    }

    @Override
    public boolean perform(@SuppressWarnings("rawtypes") AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        // This is where you 'build' the project.
        // Since this is a dummy, we just say 'hello world' and call that a build.

        // This also shows how you can consult the global configuration of the builder
    	
    	EnvVars environment = build.getEnvironment(listener);
        String workspacePath = environment.get("WORKSPACE");
        String filePath = workspacePath + File.separator + this.filePath;
        
        
        
        listener.getLogger().println("reading file: " + filePath);
      
        File file = new File(workspacePath  + File.separator + this.filePath);
        if (!file.exists()) {
        	listener.getLogger().println("the file does not exist: " + workspacePath + File.separator + this.filePath);
        	return true;
		}
        
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        String str = new String(data, "UTF-8");
        
        String lines[] = this.injectedProperties.trim().split("\\r?\\n");
        for (String line : lines) {
        	
        	if (line.trim().equals("")) {
				continue;
			}
        	
        	String sep = ":";
        	if (line.contains("=")) {
				sep = "=";
			}
        	
        	String items[] = line.split(sep);
        	Pattern p = Pattern.compile(items[0] + ":?=?.*");
    		Matcher m = p.matcher(str);
    		if (!m.find()) {
    			listener.getLogger().println("-! not found property: " + line);
    		}
    		else{
    			listener.getLogger().println("-- replacing the property: " + line);
    			str = str.replaceAll(Pattern.quote(items[0]) + ":?=?.*", Matcher.quoteReplacement(line));	
    		}
		}
        
		FileWriter writer = new FileWriter(file, false); // true to append // false to overwrite.
		writer.write(str);
		writer.close();

        return true;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link Main}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param path
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
        public FormValidation doCheckFilePath(@QueryParameter String filePath) throws IOException, ServletException {
            if (filePath.length() == 0)
                return FormValidation.error("Please set a valid path");
            return FormValidation.ok();
        }
        
        public FormValidation doCheckinjectedProperties(@QueryParameter String injectedProperties) throws IOException, ServletException {
            if (injectedProperties.length() == 0)
                return FormValidation.error("Please set expression");
            return FormValidation.ok();
        }

        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Inject property file";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            //useFrench = formData.getBoolean("useFrench");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

    }
}

