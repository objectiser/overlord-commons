/*
 * Copyright 2013 JBoss Inc
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
package org.overlord.commons.dev.server;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.overlord.commons.dev.server.discovery.IModuleDiscoveryStrategy;
import org.overlord.commons.dev.server.discovery.ModuleDiscoveryContext;

/**
 * Holds information about the development runtime environment.
 * @author eric.wittmann@redhat.com
 */
public abstract class DevServerEnvironment {

    private File targetDir;
    private Map<String, DevServerModule> modules = new HashMap<String, DevServerModule>();
    private boolean usingClassHiderAgent = false;

    /**
     * Constructor.
     * @param args
     */
    public DevServerEnvironment(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                DevServerEnvironment.this.onVmExit();
            }
        }));

        findTargetDir();
        detectAgent();
        inspectArgs(args);
    }

    /**
     * Do any cleanup on exit.
     */
    protected void onVmExit() {
        for (DevServerModule module : modules.values()) {
            module.clean();
        }
    }

    /**
     * @param moduleId
     * @return true if the module was detected in the IDE
     */
    public boolean isModuleInIDE(String moduleId) {
        DevServerModule module = getModule(moduleId);
        return module.isInIDE();
    }

    /**
     * Gets the module's work directory.
     * @param moduleId
     */
    public File getModuleWorkDir(String moduleId) {
        DevServerModule module = getModule(moduleId);
        return module.getWorkDir();
    }

    /**
     * Gets the module's directory.
     * @param moduleId
     */
    public File getModuleDir(String moduleId) {
        DevServerModule module = getModule(moduleId);
        return module.getModuleDir();
    }

    /**
     * @param moduleId
     * @return
     */
    public DevServerModule getModule(String moduleId) {
        DevServerModule module = this.modules.get(moduleId);
        if (module == null)
            throw new RuntimeException("Module not found: " + moduleId + " (perhaps it wasn't added via addAndConfigureModules()?)"); //$NON-NLS-1$ //$NON-NLS-2$
        return module;
    }

    /**
     * Adds a module to the environment.
     * @param moduleId
     * @param discoveryStrategies
     */
    public void addModule(String moduleId, IModuleDiscoveryStrategy ... discoveryStrategies) {
        System.out.println("---------------------"); //$NON-NLS-1$
        System.out.println("Discovering module: " + moduleId); //$NON-NLS-1$
        ModuleDiscoveryContext ctx = new ModuleDiscoveryContext(getTargetDir());
        for (IModuleDiscoveryStrategy strategy : discoveryStrategies) {
            DevServerModule module = strategy.discover(ctx);
            if (module != null) {
                modules.put(moduleId, module);
                System.out.println("Module found from " + strategy.getName()); //$NON-NLS-1$
                System.out.println("---------------------\n"); //$NON-NLS-1$
                return;
            }
        }
        throw new RuntimeException("Module not found: " + moduleId); //$NON-NLS-1$
    }

    /**
     * @return the targetDir
     */
    public File getTargetDir() {
        return targetDir;
    }

    /**
     * @param targetDir the targetDir to set
     */
    public void setTargetDir(File targetDir) {
        this.targetDir = targetDir;
    }

    /**
     * @return the maven target dir
     */
    private void findTargetDir() {
        String path = getClass().getClassLoader()
                .getResource(getClass().getName().replace('.', '/') + ".class").getPath(); //$NON-NLS-1$
        if (path == null) {
            throw new RuntimeException("Failed to find target directory."); //$NON-NLS-1$
        }
        if (path.contains("/target/")) { //$NON-NLS-1$
            path = path.substring(0, path.indexOf("/target/")) + "/target"; //$NON-NLS-1$ //$NON-NLS-2$
            targetDir = new File(path);
            System.out.println("Detected runtime 'target' directory: " + targetDir); //$NON-NLS-1$
        } else {
            throw new RuntimeException("Failed to find target directory."); //$NON-NLS-1$
        }
    }

    /**
     * Checks for interesting command line args.
     * @param args
     */
    protected void inspectArgs(String[] args) {
    }

    /**
     * @return the usingClassHiderAgent
     */
    public boolean isUsingClassHiderAgent() {
        return usingClassHiderAgent;
    }

    /**
     * @param usingClassHiderAgent the usingClassHiderAgent to set
     */
    public void setUsingClassHiderAgent(boolean usingClassHiderAgent) {
        this.usingClassHiderAgent = usingClassHiderAgent;
    }

    /**
     * Checks for the existence of the java agent.
     */
    private void detectAgent() {
        try {
            Class.forName("org.jboss.errai.ClientLocalClassHidingAgent"); //$NON-NLS-1$
            this.usingClassHiderAgent = true;
        } catch (ClassNotFoundException e) {
            this.usingClassHiderAgent = false;
        }
    }

    /**
     * Creates the UI application configs and sets the system property telling the Overlord
     * Header servlet where to find them.
     * @throws Exception
     */
    public void createAppConfigs() throws Exception {
        File dir = new File(this.targetDir, "overlord-apps"); //$NON-NLS-1$
        if (dir.isDirectory()) {
            FileUtils.deleteDirectory(dir);
        }
        dir.mkdirs();

        File configFile1 = new File(dir, "srampui-overlordapp.properties"); //$NON-NLS-1$
        Properties props = new Properties();
        props.setProperty("overlordapp.app-id", "s-ramp-ui"); //$NON-NLS-1$ //$NON-NLS-2$
        props.setProperty("overlordapp.href", "/s-ramp-ui/index.html");// + (ide_srampUI ? "?gwt.codesvr=127.0.0.1:9997" : "")); //$NON-NLS-1$ //$NON-NLS-2$
        props.setProperty("overlordapp.label", "S-RAMP"); //$NON-NLS-1$ //$NON-NLS-2$
        props.setProperty("overlordapp.primary-brand", "JBoss Overlord"); //$NON-NLS-1$ //$NON-NLS-2$
        props.setProperty("overlordapp.secondary-brand", "S-RAMP Repository"); //$NON-NLS-1$ //$NON-NLS-2$
        props.store(new FileWriter(configFile1), "S-RAMP UI application"); //$NON-NLS-1$

        File configFile2 = new File(dir, "dtgov-overlordapp.properties"); //$NON-NLS-1$
        props = new Properties();
        props.setProperty("overlordapp.app-id", "dtgov"); //$NON-NLS-1$ //$NON-NLS-2$
        props.setProperty("overlordapp.href", "/dtgov/index.html");// + (ide_dtgovUI ? "?gwt.codesvr=127.0.0.1:9997" : "")); //$NON-NLS-1$ //$NON-NLS-2$
        props.setProperty("overlordapp.label", "DTGov"); //$NON-NLS-1$ //$NON-NLS-2$
        props.setProperty("overlordapp.primary-brand", "JBoss Overlord"); //$NON-NLS-1$ //$NON-NLS-2$
        props.setProperty("overlordapp.secondary-brand", "Design Time Governance"); //$NON-NLS-1$ //$NON-NLS-2$
        props.store(new FileWriter(configFile2), "DTGov UI application"); //$NON-NLS-1$

        File configFile3 = new File(dir, "gadgets-overlordapp.properties"); //$NON-NLS-1$
        props = new Properties();
        props.setProperty("overlordapp.app-id", "gadget-server"); //$NON-NLS-1$ //$NON-NLS-2$
        props.setProperty("overlordapp.href", "/gadget-web/Application.html"); //$NON-NLS-1$ //$NON-NLS-2$
        props.setProperty("overlordapp.label", "Gadget Server"); //$NON-NLS-1$ //$NON-NLS-2$
        props.setProperty("overlordapp.primary-brand", "JBoss Overlord"); //$NON-NLS-1$ //$NON-NLS-2$
        props.setProperty("overlordapp.secondary-brand", "Gadget Server"); //$NON-NLS-1$ //$NON-NLS-2$
        props.store(new FileWriter(configFile3), "Gadget Server UI application"); //$NON-NLS-1$

        System.setProperty("org.overlord.apps.config-dir", dir.getCanonicalPath()); //$NON-NLS-1$
        System.out.println("Generated app configs in: " + dir.getCanonicalPath()); //$NON-NLS-1$
    }

}
