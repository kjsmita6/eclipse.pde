/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.plugin;

import org.eclipse.core.runtime.*;
/**
 * The class that implements this interface represents a
 * reference to the library that is defined in the plug-in
 * manifest.
 */
public interface IPluginLibrary extends IPluginObject {
	/**
	 * A name of the property that will be used to
	 * notify about changes of the "exported" field.
	 */
	String P_EXPORTED = "export"; //$NON-NLS-1$
	/**
	 * A name of the property that will be used to
	 * notify about changes in the content filters.
	 */
	String P_PACKAGES = "packages"; //$NON-NLS-1$
	
	/**
	 * A name of the property that will be used to
	 * notify about changes in the content filters.
	 */
	String P_CONTENT_FILTERS = "contentFilters"; //$NON-NLS-1$
	/**
	 * A name of the property that will be used to
	 * notify about of the 'type' field.
	 */
	String P_TYPE = "type"; //$NON-NLS-1$
	/**
	 * A library type indicating the library contains code.
	 */
	String CODE = "code"; //$NON-NLS-1$
	/**
	 * A library type indicating the library contains resource files.
	 */
	String RESOURCE = "resource"; //$NON-NLS-1$
	/**
	 * Returns optional context filters that
	 * should be applied to calculate what classes
	 * to export from this library.
	 *
	 * @return an array of content filter strings
	 */
	String[] getContentFilters();
	
	/**
	 * Returns optional package prefixes that can be used
	 * to make library lookup faster..
	 *
	 * @return an array of package prefixes
	 */
	String[] getPackages();
	/**
	 * Returns true if this library contains types
	 * that will be visible to other plug-ins.
	 *
	 * @return true if there are exported types in the library
	 */
	boolean isExported();
	/**
	 * Returns true if all the types in this library
	 * will be visible to other plug-ins.
	 *
	 * @return true if all the types are exported
	 * in the library
	 */
	boolean isFullyExported();

	/**
	 * Returns a type of this library (CODE or RESOURCE)
	 */
	String getType();
	/**
	 * Sets the optional content filters for
	 * this library. This method may throw
	 * a CoreException if the model is not
	 * editable.
	 *
	 * @param filters an array of filter strings
	 */
	void setContentFilters(String[] filters) throws CoreException;
	
	/**
	 * Export a particular package in a library. 
	 * This method may throw a CoreException if 
	 * the model is not editable.
	 *
	 * @param filter a package name
	 */
	void addContentFilter(String filter) throws CoreException;

	/**
	 * Remove a package from the export list. 
	 * This method may throw a CoreException if 
	 * the model is not editable.
	 *
	 * @param filter a package name
	 */
	void removeContentFilter(String filter) throws CoreException;
	
	
	/**
	 * Sets the optional package prefixes for this library.
	 * This method may throw a CoreException if the model is not 
	 * editable.
	 *
	 * @param packages an array of package prefixes
	 */
	void setPackages(String[] packages) throws CoreException;
	/**
	 * Sets whether types in this library will be
	 * visible to other plug-ins. This method
	 * may throw a CoreException if the model is
	 * not editable.
	 */
	void setExported(boolean value) throws CoreException;
	/**
	 * Sets the library type. Must be either CODE or RESOURCE.
	 * @throws CoreException if the model is not editable.
	 */
	void setType(String type) throws CoreException;
}
