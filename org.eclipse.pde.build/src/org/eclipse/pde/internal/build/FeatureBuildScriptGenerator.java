/**********************************************************************
 * Copyright (c) 2000, 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build;

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.PluginModel;
import org.eclipse.core.runtime.model.PluginPrerequisiteModel;
import org.eclipse.pde.internal.build.ant.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.FeatureExecutableFactory;

/**
 * Generates build.xml script for features.
 */
public class FeatureBuildScriptGenerator extends AbstractBuildScriptGenerator {
	
	/**
	 * Indicates whether scripts for this feature's children should be generated.
	 */
	protected boolean generateChildrenScript = true;
	/**
	 * 
	 */
	protected String featureID;
	/**
	 * Where to get the feature description from.
	 */
	protected String featureRootLocation;
	/**
	 * Target feature.
	 */
	protected Feature feature;

	protected static final String FEATURE_FULL_NAME = getPropertyFormat(PROPERTY_FEATURE_FULL_NAME);
	protected static final String SOURCE_FEATURE_FULL_NAME = getPropertyFormat(PROPERTY_FEATURE) + ".source" + getPropertyFormat(PROPERTY_FEATURE_VERSION_SUFFIX);
	protected static final String FEATURE_FOLDER_NAME = "features/" + FEATURE_FULL_NAME;

/**
 * Returns a list of PluginModel objects representing the elements. The boolean
 * argument indicates whether the list should consist of plug-ins or fragments.
 */
protected List computeElements(boolean fragments) throws CoreException {
	List result = new ArrayList(5);
	IPluginEntry[] pluginList = feature.getPluginEntries();
	for (int i = 0; i < pluginList.length; i++) {
		IPluginEntry entry = pluginList[i];
		if (fragments == entry.isFragment()) { // filter the plugins or fragments
			VersionedIdentifier identifier = entry.getVersionedIdentifier();
			PluginModel model;
			if (fragments)
				model = getRegistry().getFragment(identifier.getIdentifier(), identifier.getVersion().toString());
			else
				model = getRegistry().getPlugin(identifier.getIdentifier(), identifier.getVersion().toString());
			if (model == null)
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, Policy.bind("exception.missingPlugin", entry.getVersionedIdentifier().toString()), null));
			else
				result.add(model);
		}
	}
	return result;
}
public void setGenerateChildrenScript(boolean generate) {
	generateChildrenScript = generate;
}
public void generate() throws CoreException {
	if (featureID == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, Policy.bind("error.missingFeatureId"), null));
	if (installLocation == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_INSTALL_LOCATION_MISSING, Policy.bind("error.missingInstallLocation"), null));

	readFeature();
	try {
		// if the feature defines its own custom script, we do not generate a new one
		// but we do try to update the version number
		String custom = (String) getBuildProperties(feature).get(PROPERTY_CUSTOM);
		if (custom != null && custom.equalsIgnoreCase("true")) {
			File buildFile = new File(getFeatureRootLocation(), buildScriptName);
			updateVersion(buildFile, PROPERTY_FEATURE_VERSION_SUFFIX, feature.getFeatureVersion());
			return;
		}
	
		if (generateChildrenScript)
			generateChildrenScripts();

		File root = new File(getFeatureRootLocation());
		File target = new File(root, buildScriptName);
		AntScript script = new AntScript(new FileOutputStream(target));
		try {
			generateBuildScript(script);
		} finally {
			script.close();
		}
	} catch (IOException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_SCRIPT, Policy.bind("exception.writeScript"), e));
	}
}
/**
 * Main call for generating the script.
 */
protected void generateBuildScript(AntScript script) throws CoreException {
	generatePrologue(script);
	generateAllPluginsTarget(script);
	generateAllFragmentsTarget(script);
	generateAllChildrenTarget(script);
	generateChildrenTarget(script);
	generateBuildJarsTarget(script);
	generateBuildZipsTarget(script);
	generateBuildUpdateJarTarget(script);
	generateGatherBinPartsTarget(script);
	generateZipDistributionWholeTarget(script);
	generateZipSourcesTarget(script);
	generateZipLogsTarget(script);
	generateCleanTarget(script);
	generateRefreshTarget(script, getPropertyFormat(PROPERTY_FEATURE));
	generateEpilogue(script);
}

/**
 * FIXME: add comments
 */
protected void generateBuildZipsTarget(AntScript script) throws CoreException {
	StringBuffer zips = new StringBuffer();
	Properties props = getBuildProperties(feature);
	for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();) {
		Map.Entry entry = (Map.Entry) iterator.next();
		String key = (String) entry.getKey();
		if (key.startsWith(PROPERTY_SOURCE_PREFIX) && key.endsWith(PROPERTY_ZIP_SUFFIX)) {
			String zipName = key.substring(PROPERTY_SOURCE_PREFIX.length());
			zips.append(',');
			zips.append(zipName);
			generateZipIndividualTarget(script, zipName, (String) entry.getValue());
		}
	}
	script.println();
	int tab = 1;
	script.printTargetDeclaration(tab++, TARGET_BUILD_ZIPS, TARGET_INIT + zips.toString(), null, null, null);
	Map params = new HashMap(2);
	params.put(PROPERTY_TARGET, TARGET_BUILD_ZIPS);
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	script.printString(--tab, "</target>");
}
/**
 * FIXME: add comments
 */
protected void generateZipIndividualTarget(AntScript script, String zipName, String source) throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, zipName, TARGET_INIT, null, null, null);
	IPath root = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	script.printZipTask(tab, root.append(zipName).toString(), root.append(source).toString(), false, null);
	script.printString(--tab, "</target>");
}
protected void generateCleanTarget(AntScript script) throws CoreException {
	int tab = 1;
	script.println();
	IPath basedir = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	script.printTargetDeclaration(tab++, TARGET_CLEAN, TARGET_INIT, null, null, null);
	script.printDeleteTask(tab, null, basedir.append(FEATURE_FULL_NAME + ".jar").toString(), null);
	script.printDeleteTask(tab, null, basedir.append(FEATURE_FULL_NAME + ".bin.dist.zip").toString(), null);
	script.printDeleteTask(tab, null, basedir.append(FEATURE_FULL_NAME + ".log.zip").toString(), null);
	script.printDeleteTask(tab, null, basedir.append(FEATURE_FULL_NAME + ".src.zip").toString(), null);
	script.printString(--tab, "</target>");
}
protected void generateZipLogsTarget(AntScript script) {
	IPath base = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	base = base.append("_temp_");
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_ZIP_LOGS, TARGET_INIT, null, null, null);
	script.printProperty(tab, PROPERTY_BASE, base.toString());
	String baseProperty = getPropertyFormat(PROPERTY_BASE);
	script.printDeleteTask(tab, baseProperty, null, null);
	script.printMkdirTask(tab, baseProperty);
	Map params = new HashMap(1);
	params.put(PROPERTY_TARGET, TARGET_GATHER_LOGS);
	params.put(PROPERTY_DESTINATION, new Path(baseProperty).append("plugins").toString());
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, "false", params);
	IPath destination = new Path(getPropertyFormat(PROPERTY_BASEDIR)).append(FEATURE_FULL_NAME + ".log.zip");
	script.printZipTask(tab, destination.toString(), baseProperty, true, null);
	script.printDeleteTask(tab, baseProperty, null, null);
	script.printString(--tab, "</target>");
}

protected void generateZipSourcesTarget(AntScript script) {
	String featurebase = getPropertyFormat(PROPERTY_FEATURE_BASE);
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab, TARGET_ZIP_SOURCES, TARGET_INIT, null, null, null);
	tab++;
	IPath destination = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	script.printProperty(tab, PROPERTY_FEATURE_BASE, destination.append("zip.sources.pdetemp").toString());
	script.printDeleteTask(tab, featurebase, null, null);
	script.printMkdirTask(tab, featurebase);
	Map params = new HashMap(1);
	params.put(PROPERTY_TARGET, TARGET_GATHER_SOURCES);
	params.put(PROPERTY_DESTINATION, new Path(featurebase).append("plugins").append(SOURCE_FEATURE_FULL_NAME).append("src").toString());
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	script.printZipTask(tab, destination.append(FEATURE_FULL_NAME + ".src.zip").toString(), featurebase, true, null);
	script.printDeleteTask(tab, featurebase, null, null);
	tab--;
	script.printString(tab, "</target>");
}
protected void generateGatherBinPartsTarget(AntScript script) throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_GATHER_BIN_PARTS, TARGET_INIT, PROPERTY_FEATURE_BASE, null, null);
	Map params = new HashMap(1);
	params.put(PROPERTY_TARGET, TARGET_GATHER_BIN_PARTS);
	params.put(PROPERTY_DESTINATION, new Path(getPropertyFormat(PROPERTY_FEATURE_BASE)).append("plugins").toString());
	script.printAntCallTask(tab, TARGET_CHILDREN, null, params);
	String include = (String) getBuildProperties(feature).get(PROPERTY_BIN_INCLUDES);
	String exclude = (String) getBuildProperties(feature).get(PROPERTY_BIN_EXCLUDES);
	String root = "${feature.base}/" + FEATURE_FOLDER_NAME;
	script.printMkdirTask(tab, root);
	if (include != null || exclude != null) {
		FileSet fileSet = new FileSet(getPropertyFormat(PROPERTY_BASEDIR), null, include, null, exclude, null, null);
		script.printCopyTask(tab, null, root, new FileSet[]{ fileSet });
	}	
	script.printString(--tab, "</target>");
}
protected void generateBuildUpdateJarTarget(AntScript script) {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_BUILD_UPDATE_JAR, TARGET_INIT, null, null, null);
	Map params = new HashMap(1);
	params.put(PROPERTY_TARGET, TARGET_BUILD_UPDATE_JAR);
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	IPath destination = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	script.printProperty(tab, PROPERTY_FEATURE_BASE, destination.append("bin.zip.pdetemp").toString());
	script.printDeleteTask(tab, getPropertyFormat(PROPERTY_FEATURE_BASE), null, null);
	script.printMkdirTask(tab, getPropertyFormat(PROPERTY_FEATURE_BASE));
	// be sure to call the gather with children turned off.  The only way to do this is 
	// to clear all inherited values.  Must remember to setup anything that is really expected.
	params.clear();
	params.put(PROPERTY_FEATURE_BASE, getPropertyFormat(PROPERTY_FEATURE_BASE));
	script.printAntCallTask(tab, TARGET_GATHER_BIN_PARTS, "false", params);
	script.printJarTask(tab, destination.append(FEATURE_FULL_NAME + ".jar").toString(), "${feature.base}");
	script.printDeleteTask(tab, getPropertyFormat(PROPERTY_FEATURE_BASE), null, null);
	script.printString(--tab, "</target>");
}
/**
 * Zip up the whole feature.
 */
protected void generateZipDistributionWholeTarget(AntScript script) {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab, TARGET_ZIP_DISTRIBUTION, TARGET_INIT, null, null, null);
	tab++;
	IPath destination = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	script.printProperty(tab, PROPERTY_FEATURE_BASE, destination.append("bin.zip.pdetemp").toString());
	script.printDeleteTask(tab, getPropertyFormat(PROPERTY_FEATURE_BASE), null, null);
	script.printMkdirTask(tab, getPropertyFormat(PROPERTY_FEATURE_BASE));
	Map params = new HashMap(1);
	params.put(PROPERTY_INCLUDE_CHILDREN, "true");
	script.printAntCallTask(tab, TARGET_GATHER_BIN_PARTS, null, params);
	script.printZipTask(tab, destination.append(FEATURE_FULL_NAME + ".bin.dist.zip").toString(), getPropertyFormat(PROPERTY_FEATURE_BASE), false, null);
	script.printDeleteTask(tab, getPropertyFormat(PROPERTY_FEATURE_BASE), null, null);
	tab--;
	script.printString(tab, "</target>");
}
/**
 * Executes a given target in all children's script files.
 */
protected void generateAllChildrenTarget(AntScript script) {
	StringBuffer depends = new StringBuffer();
	depends.append(TARGET_INIT);
	depends.append(",");
	depends.append(TARGET_ALL_PLUGINS);
	depends.append(",");
	depends.append(TARGET_ALL_FRAGMENTS);
	
	script.println();
	script.printTargetDeclaration(1, TARGET_ALL_CHILDREN, depends.toString(), null, null, null);
	script.printString(1, "</target>");
}
/**
 * Target responsible for delegating target calls to plug-in's build.xml scripts.
 */
protected void generateAllPluginsTarget(AntScript script) throws CoreException {
	int tab = 1;
	List plugins = computeElements(false);
	String[][] sortedPlugins = Utils.computePrerequisiteOrder((PluginModel[]) plugins.toArray(new PluginModel[plugins.size()]));
	script.println();
	script.printTargetDeclaration(tab++, TARGET_ALL_PLUGINS, TARGET_INIT, null, null, null);
	for (int list = 0; list < 2; list++) {
		for (int i = 0; i < sortedPlugins[list].length; i++) {
			PluginModel plugin = getRegistry().getPlugin(sortedPlugins[list][i]);
			IPath location = Utils.makeRelative(new Path(getLocation(plugin)), new Path(getFeatureRootLocation()));
			script.printAntTask(tab, buildScriptName, location.toString(), getPropertyFormat(PROPERTY_TARGET), null, null, null);
		}
	}
	script.printString(--tab, "</target>");
}
/**
 * Target responsible for delegating target calls to fragments's build.xml scripts.
 */
protected void generateAllFragmentsTarget(AntScript script) throws CoreException {
	int tab = 1;
	List fragments = computeElements(true);
	script.println();
	script.printTargetDeclaration(tab++, TARGET_ALL_FRAGMENTS, TARGET_INIT, null, null, null);
	for (Iterator iterator = fragments.iterator(); iterator.hasNext();) {
		PluginModel fragment = (PluginModel) iterator.next();
		IPath location = Utils.makeRelative(new Path(getLocation(fragment)), new Path(getFeatureRootLocation()));
		script.printAntTask(tab, buildScriptName, location.toString(), getPropertyFormat(PROPERTY_TARGET), null, null, null);
	}
	script.printString(--tab, "</target>");
}





/**
 * Just ends the script.
 */
protected void generateEpilogue(AntScript script) {
	script.println();
	script.printString(0, "</project>");
}
/**
 * Defines, the XML declaration, Ant project and init target.
 */
protected void generatePrologue(AntScript script) {
	int tab = 1;
	script.printProjectDeclaration(feature.getFeatureIdentifier(), TARGET_INIT, ".");
	script.println();
	script.printTargetDeclaration(tab++, TARGET_INIT, null, null, null, null);
	script.printProperty(tab, PROPERTY_FEATURE, feature.getFeatureIdentifier());
	script.printProperty(tab, PROPERTY_FEATURE_VERSION_SUFFIX, "_" + feature.getFeatureVersion());
	script.printProperty(tab, PROPERTY_FEATURE_FULL_NAME, getPropertyFormat(PROPERTY_FEATURE) + getPropertyFormat(PROPERTY_FEATURE_VERSION_SUFFIX));
	script.printString(--tab, "</target>");
}
protected void generateChildrenScripts() throws CoreException {
	generateModels(new PluginBuildScriptGenerator(), computeElements(false));
	generateModels(new PluginBuildScriptGenerator(), computeElements(true));
}
protected void generateModels(ModelBuildScriptGenerator generator, List models) throws CoreException {
	if (models.isEmpty())
		return;
	generator.setInstallLocation(installLocation);
	generator.setDevEntries(devEntries);
	generator.setPluginPath(getPluginPath());
	for (Iterator iterator = models.iterator(); iterator.hasNext();) {
		PluginModel model = (PluginModel) iterator.next();
		// setModel has to be called before configurePersistentProperties
		// because it reads the model's properties
		generator.setModel(model);
		generator.generate();
	}
}
public void setFeature(String featureID) throws CoreException {
	if (featureID == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, Policy.bind("error.missingFeatureId"), null));
	this.featureID = featureID;
}
/**
 * Reads the target feature from the specified location.
 */
protected void readFeature() throws CoreException {
	String location = getFeatureRootLocation();
	if (location == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, Policy.bind("error.missingFeatureLocation"), null));
	
	FeatureExecutableFactory factory = new FeatureExecutableFactory();
	File file = new File(location);
	try {
		feature = (Feature) factory.createFeature(file.toURL(), null);
		if (feature == null)
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, Policy.bind("error.creatingFeature", new String[] {featureID}), null));	
	} catch (MalformedURLException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, Policy.bind("error.creatingFeature", new String[] {featureID}), e));
	}
}
/**
 * If the feature location was not specified, use a default one.
 */
protected String getFeatureRootLocation() {
	if (featureRootLocation == null) {
		IPath location = new Path(installLocation);
		location = location.append(DEFAULT_FEATURE_LOCATION);
		location = location.append(featureID);
		featureRootLocation = location.addTrailingSeparator().toOSString();
	}
	return featureRootLocation;
}
/**
 *
 */
public void setFeatureRootLocation(String location) {
	this.featureRootLocation = location;
}

protected Properties getBuildProperties(Feature feature) throws CoreException {
	VersionedIdentifier identifier = feature.getVersionedIdentifier();
	Properties result = (Properties) buildProperties.get(identifier);
	if (result == null) {
		result = readBuildProperties(getFeatureRootLocation());
		buildProperties.put(identifier, result);
	}
	return result;
}
/**
 * Delegates some target call to all-template only if the property
 * includeChildren is set.
 */
protected void generateChildrenTarget(AntScript script) {
	script.println();
	script.printTargetDeclaration(1, TARGET_CHILDREN, null, PROPERTY_INCLUDE_CHILDREN, null, null);
	script.printAntCallTask(2, TARGET_ALL_CHILDREN, null, null);
	script.printString(1, "</target>");
}

protected void generateBuildJarsTarget(AntScript script) throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_BUILD_JARS, TARGET_INIT, null, null, null);
	Map params = new HashMap(1);
	params.put(PROPERTY_TARGET, TARGET_BUILD_JARS);
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	script.printEndTag(--tab, "target");
	script.println();
	script.printTargetDeclaration(tab++, TARGET_BUILD_SOURCES, TARGET_INIT, null, null, null);
	params.clear();
	params.put(PROPERTY_TARGET, TARGET_BUILD_SOURCES);
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	script.printEndTag(--tab, "target");
}

}