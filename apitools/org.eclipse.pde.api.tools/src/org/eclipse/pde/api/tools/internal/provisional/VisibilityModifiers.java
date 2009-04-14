/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional;

import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Class containing constants and utility methods for visibility modifiers
 * 
 * @since 1.0.0
 */
public final class VisibilityModifiers {
	
	/**
	 * Visibility constant indicating an element is public API.
	 */
	public static final int API = 0x0001;
	/**
	 * Visibility constant indicating a element should not be referenced. This 
	 * indicates the element is internal and not intended for general use.
	 */
	public static final int PRIVATE = 0x0002;
	/**
	 * Visibility constant indicating an element is public for a specific
	 * audience (service provider interface).
	 */
	public static final int SPI = 0x0004;
	
	/**
	 * Visibility constant indicating an element is private, but some
	 * clients have been permitted access to the element.
	 */
	public static final int PRIVATE_PERMISSIBLE = 0x0008;
	
	/**
	 * Visibility constant indicating an element has host-fragment level of visibility.
	 *  i.e. fragments have {@link #PRIVATE_PERMISSIBLE}-like access to the internals of their host.
	 *  
	 *  @since 1.0.1
	 */
	public static final int FRAGMENT_PERMISSIBLE = 0x0016;
	
	/**
	 * Bit mask of all visibilities.
	 */
	public static final int ALL_VISIBILITIES = 0xFFFF;
	
	/**
	 * Constructor
	 * no instantiating
	 */
	private VisibilityModifiers() {}
	
	/**
	 * Returns if the modifier is 'API'
	 * 
	 * @param modifiers the modifiers to resolve
	 * @return if the modifier is 'API'
	 */
	public static final boolean isAPI(int modifiers) {
		return (modifiers & API) > 0;
	}
	
	/**
	 * Returns if the modifier is 'SPI'
	 * 
	 * @param modifiers the modifiers to resolve
	 * @return if the modifier is 'SPI'
	 */
	public static final boolean isSPI(int modifiers) {
		return (modifiers & SPI) > 0;
	}
	
	/**
	 * Returns if the modifier is 'Private'
	 * 
	 * @param modifiers the modifiers to resolve
	 * @return if the modifier is 'Private'
	 */
	public static final boolean isPrivate(int modifiers) {
		return (modifiers & PRIVATE) > 0;
	}
	
	/**
	 * Returns if the modifier is 'Private Permissible'
	 * 
	 * @param modifiers the modifiers to resolve
	 * @return if the modifier is 'Private Permissible'
	 */
	public static final boolean isPermissiblePrivate(int modifiers) {
		return (modifiers & PRIVATE_PERMISSIBLE) > 0;
	}	
	
	/**
	 * Returns if the modifiers is 'Fragment Permissible'
	 * 
	 * @param modifiers the modifiers to resolve
	 * @return if the modifiers is 'Fragment Permissible'
	 * @since 1.0.1
	 */
	public static final boolean isFragmentPermissible(int modifiers) {
		return (modifiers & FRAGMENT_PERMISSIBLE) > 0;
	}

	/**
	 * Returns the string representation of the specified visibility modifier or <code>UNKNOWN_VISIBILITY</code>
	 * if the modifier is unknown.
	 * 
	 * @param visibility
	 * @return the string representation of the visibility or <code>UNKNOWN_VISIBILITY</code>
	 * @since 1.0.1
	 */
	public static String getVisibilityName(int visibility) {
		switch(visibility) {
			case ALL_VISIBILITIES: {
				return "ALL_VISIBILITIES"; //$NON-NLS-1$
			}
			case API: {
				return "API"; //$NON-NLS-1$
			}
			case PRIVATE: {
				return "PRIVATE"; //$NON-NLS-1$
			}
			case PRIVATE_PERMISSIBLE: {
				return "PRIVATE_PERMISSIBLE"; //$NON-NLS-1$
			}
			case FRAGMENT_PERMISSIBLE: {
				return "FRAGMENT_PERMISSIBLE"; //$NON-NLS-1$
			}
			case SPI: {
				return "SPI"; //$NON-NLS-1$
			}
			case 0: {
				return "INHERITED"; //$NON-NLS-1$
			}
		}
		return Util.UNKNOWN_VISIBILITY;
	}
}
